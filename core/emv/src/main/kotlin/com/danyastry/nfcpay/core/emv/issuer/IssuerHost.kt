package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CardFiles
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.crypto.Arpc
import com.danyastry.nfcpay.core.emv.crypto.IccKeyDerivation
import com.danyastry.nfcpay.core.emv.crypto.KeyCheckValue
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.hsm.PinCheck
import com.danyastry.nfcpay.core.emv.hsm.PinService
import com.danyastry.nfcpay.core.emv.dol.DolEntry
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException
import com.danyastry.nfcpay.core.emv.token.DetokenizeResult
import com.danyastry.nfcpay.core.emv.token.TokenDomain
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.util.Bcd
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import java.time.YearMonth

class IssuerHost(
    private val issuerMasterKey: ByteArray,
    accounts: List<IssuerAccount>,
    private val cdol1: List<DolEntry> = CardFiles.CDOL1,
    private val tokens: TokenService? = null,
    private val pins: PinService? = null,
) {
    private val accounts = accounts.associateBy { key(it.pan, it.panSequenceNumber) }.toMutableMap()
    private val tokenCounters = mutableMapOf<String, Int>()
    private val keyUses = mutableMapOf<String, Int>()

    @Synchronized
    fun account(pan: String, panSequenceNumber: Int): IssuerAccount? = accounts[key(pan, panSequenceNumber)]

    @Synchronized
    fun reset(accounts: List<IssuerAccount>) {
        this.accounts.clear()
        tokenCounters.clear()
        keyUses.clear()
        accounts.forEach { pins?.unblock(it.pan) }
        accounts.forEach { this.accounts[key(it.pan, it.panSequenceNumber)] = it }
    }

    @Synchronized
    fun authorize(request: AuthorizationRequest): AuthorizationResponse = Session(request).run()

    private inner class Session(private val request: AuthorizationRequest) {
        private val steps = mutableListOf<VerificationStep>()
        private var amount: Long? = null
        private var atc: Int? = null

        fun run(): AuthorizationResponse {
            val values = try {
                BerTlv.parse(request.field55).associate { it.tag to it.value }
            } catch (_: TlvFormatException) {
                return decline(DeclineReason.MISSING_DATA, "Field 55 is not valid TLV")
            }
            val required = cdol1.map { it.tag } + REQUIRED_TAGS
            val missing = required.filterNot { it in values }
            if (missing.isNotEmpty()) return decline(DeclineReason.MISSING_DATA, "Missing ${missing.joinToString { it.hex }}")
            step("Field 55", "${values.size} tags, all data the cryptogram covers is present", passed = true)

            val service = tokens
            val token = service?.takeIf { it.vault.isToken(request.pan) }?.let {
                when (val result = it.vault.detokenize(request.pan, TokenDomain.CONTACTLESS, request.date)) {
                    is DetokenizeResult.Refused -> return decline(DeclineReason.TOKEN_REFUSED, result.reason.description)
                    is DetokenizeResult.Found -> result.record
                }
            }
            if (token != null) {
                step(
                    "Token",
                    "${SensitiveData.maskPan(token.token)} → ${SensitiveData.maskPan(token.pan)} · PAR ${token.paymentAccountReference}",
                    passed = true,
                )
            }
            val account = (if (token != null) account(token.pan, token.panSequenceNumber) else account(request.pan, request.panSequenceNumber))
                ?: return decline(DeclineReason.UNKNOWN_ACCOUNT, SensitiveData.maskPan(request.pan))
            step("Account", "${SensitiveData.maskPan(account.pan)} · sequence ${account.panSequenceNumber}", passed = true)

            val iad = values.getValue(EmvTags.ISSUER_APPLICATION_DATA)
            val version = iad.takeIf { it.size >= CardCryptogram.IAD_LENGTH }?.let { CryptogramVersion.of(it[CardCryptogram.CVN_OFFSET].toInt() and 0xFF) }
                ?: return decline(DeclineReason.UNKNOWN_CRYPTOGRAM_VERSION, "IAD ${iad.toHex()}")
            val counter = values.getValue(EmvTags.ATC).toUnsignedInt().also { atc = it }
            val requested = Bcd.decodeNumber(values.getValue(EmvTags.AMOUNT_AUTHORISED)).also { amount = it }

            if ((token != null) != (version == CryptogramVersion.LIMITED_USE_KEY)) {
                return decline(DeclineReason.WRONG_KEY_TYPE, version.label)
            }
            var keyUse: Pair<String, Int>? = null
            val key = if (token != null && service != null) {
                val index = iad[CardCryptogram.DKI_OFFSET].toInt() and 0xFF
                val limitedUseKey = service.keyFor(token.token, index)
                    ?: return decline(DeclineReason.KEY_RETIRED, "Key index $index, active ${service.activeKeyIndex(token.token)}")
                val useKey = "${token.token}/$index"
                val used = keyUses[useKey] ?: 0
                if (used >= service.usesPerKey) {
                    return decline(DeclineReason.KEY_EXHAUSTED, "Key index $index already signed $used of ${service.usesPerKey} taps")
                }
                keyUse = useKey to used + 1
                step(
                    "Limited-use key",
                    "${version.label} · index $index · tap ${used + 1} of ${service.usesPerKey} · KCV ${KeyCheckValue.of(limitedUseKey)}",
                    passed = true,
                )
                limitedUseKey
            } else {
                val cardKey = IccKeyDerivation.optionA(issuerMasterKey, account.pan, account.panSequenceNumber)
                step("Card key", "Option A from the issuer master key · KCV ${KeyCheckValue.of(cardKey)}", passed = true)
                CardCryptogram.key(cardKey, version, counter).also {
                    step("Cryptogram key", "${version.label} · ATC $counter · KCV ${KeyCheckValue.of(it)}", passed = true)
                }
            }

            val cvr = iad.copyOfRange(CardCryptogram.CVR_OFFSET, CardCryptogram.IAD_LENGTH)
            val data = Dol.build(cdol1) { values[it] }
            val expected = CardCryptogram.compute(key, data, values.getValue(EmvTags.AIP), counter, cvr)
            val received = values.getValue(EmvTags.APPLICATION_CRYPTOGRAM)
            if (!expected.contentEquals(received)) {
                return decline(DeclineReason.CRYPTOGRAM_MISMATCH, "Expected ${expected.toHex()}, got ${received.toHex()}")
            }
            step("ARQC", "${received.toHex()} matches the recomputed MAC", passed = true)

            val lastAtc = if (token != null) tokenCounters[token.token] ?: IssuerAccount.NO_TRANSACTIONS else account.lastAtc
            if (counter <= lastAtc) {
                return decline(DeclineReason.REPLAYED_COUNTER, "ATC $counter, last seen $lastAtc")
            }
            step("Replay check", "ATC $counter is newer than ${lastAtc.takeIf { it >= 0 } ?: "none"}", passed = true)
            val counted = if (token != null) {
                tokenCounters[token.token] = counter
                keyUse?.let { (useKey, uses) -> keyUses[useKey] = uses }
                account
            } else {
                account.copy(lastAtc = counter)
            }
            accounts[key(account.pan, account.panSequenceNumber)] = counted

            val reason = cardDecision(account, values) ?: checkPin() ?: fundsDecision(account, requested)
            val responseCode = reason?.responseCode ?: ResponseCode.APPROVED
            if (reason == null) {
                accounts[key(account.pan, account.panSequenceNumber)] = counted.copy(balance = account.balance - requested)
                step("Funds", "${money(requested)} of ${money(account.balance)} available", passed = true)
            } else {
                step(reason.description, responseCode.description, passed = false)
            }

            val arpc = Arpc.method1(key, received, responseCode.bytes)
            step("ARPC", "Method 1 over ARQC and response code ${responseCode.code}: ${arpc.toHex()}", passed = true)
            return AuthorizationResponse(
                responseCode = responseCode,
                declineReason = reason,
                amount = requested,
                atc = counter,
                arpc = arpc.toHex(),
                issuerAuthenticationData = (arpc + responseCode.bytes).toHex(),
                steps = steps.toList(),
            )
        }

        private fun cardDecision(account: IssuerAccount, values: Map<Tag, ByteArray>): DeclineReason? {
            val cid = values.getValue(EmvTags.CRYPTOGRAM_INFORMATION_DATA).firstOrNull()?.toInt()?.and(0xFF) ?: 0
            return when {
                CryptogramType.fromBits(cid) == CryptogramType.AAC -> DeclineReason.CARD_DECLINED
                account.expiry.isBefore(YearMonth.from(request.date)) -> DeclineReason.EXPIRED_CARD
                else -> null
            }
        }

        private fun fundsDecision(account: IssuerAccount, requested: Long): DeclineReason? =
            if (requested > account.balance) DeclineReason.INSUFFICIENT_FUNDS else null

        private fun checkPin(): DeclineReason? {
            val encrypted = request.onlinePin ?: return null
            val verdict = pins?.verify(request.pan, encrypted) ?: return DeclineReason.UNREADABLE_PIN
            val detail = "KSN ${encrypted.ksn} · ${verdict.check?.description ?: "PIN blocked"} · tries left ${verdict.triesLeft}"
            return when {
                verdict.isVerified -> null.also { step("Online PIN", detail, passed = true) }
                verdict.check == PinCheck.WRONG_PIN && !verdict.blocked -> DeclineReason.INCORRECT_PIN.also { step("Online PIN", detail, passed = false) }
                verdict.blocked -> DeclineReason.PIN_TRIES_EXCEEDED.also { step("Online PIN", detail, passed = false) }
                else -> DeclineReason.UNREADABLE_PIN.also { step("Online PIN", detail, passed = false) }
            }
        }

        private fun step(title: String, detail: String, passed: Boolean) {
            steps += VerificationStep(title, detail, passed)
        }

        private fun decline(reason: DeclineReason, detail: String): AuthorizationResponse {
            step(reason.description, detail, passed = false)
            return AuthorizationResponse(reason.responseCode, reason, amount, atc, arpc = null, issuerAuthenticationData = null, steps = steps.toList())
        }
    }

    private fun key(pan: String, panSequenceNumber: Int) = "$pan/$panSequenceNumber"

    private fun money(minor: Long): String = "%d.%02d".format(minor / 100, minor % 100)

    private companion object {
        val REQUIRED_TAGS = listOf(
            EmvTags.APPLICATION_CRYPTOGRAM,
            EmvTags.CRYPTOGRAM_INFORMATION_DATA,
            EmvTags.ISSUER_APPLICATION_DATA,
            EmvTags.ATC,
            EmvTags.AIP,
        )
    }
}
