package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CardFiles
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.IssuerApplicationData
import com.danyastry.nfcpay.core.emv.crypto.Arpc
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.dol.DolEntry
import com.danyastry.nfcpay.core.emv.hsm.PinCheck
import com.danyastry.nfcpay.core.emv.hsm.PinService
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.response.ResponseCode
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException
import com.danyastry.nfcpay.core.emv.token.DetokenizeResult
import com.danyastry.nfcpay.core.emv.token.TokenDomain
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.util.Bcd
import com.danyastry.nfcpay.core.emv.util.Money
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
    private val ledger = TokenLedger()

    @Synchronized
    fun account(pan: String, panSequenceNumber: Int): IssuerAccount? = accounts[key(pan, panSequenceNumber)]

    @Synchronized
    fun reset(accounts: List<IssuerAccount>) {
        this.accounts.clear()
        ledger.clear()
        accounts.forEach { pins?.unblock(it.pan) }
        accounts.forEach { this.accounts[key(it.pan, it.panSequenceNumber)] = it }
    }

    @Synchronized
    fun authorize(request: AuthorizationRequest): AuthorizationResponse = Session(request).run()

    private inner class Session(private val request: AuthorizationRequest) {
        private val steps = mutableListOf<VerificationStep>()
        private var amount: Long? = null
        private var atc: Int? = null

        fun run(): AuthorizationResponse = try {
            authorize()
        } catch (decline: AuthorizationDecline) {
            decline(decline.reason, decline.detail)
        }

        private fun authorize(): AuthorizationResponse {
            val values = try {
                BerTlv.parse(request.field55).associate { it.tag to it.value }
            } catch (_: TlvFormatException) {
                return decline(DeclineReason.MISSING_DATA, "Field 55 is not valid TLV")
            }
            val required = cdol1.map { it.tag } + REQUIRED_TAGS
            val missing = required.filterNot { it in values }
            if (missing.isNotEmpty()) return decline(DeclineReason.MISSING_DATA, "Missing ${missing.joinToString { it.hex }}")
            step("Field 55", "${values.size} tags, all data the cryptogram covers is present", passed = true)

            val credential = resolveCredential()
            val account = credential.account
            step("Account", "${SensitiveData.maskPan(account.pan)} - sequence ${account.panSequenceNumber}", passed = true)

            val rawIad = values.getValue(EmvTags.ISSUER_APPLICATION_DATA)
            val iad = IssuerApplicationData.parse(rawIad)
                ?: return decline(DeclineReason.UNKNOWN_CRYPTOGRAM_VERSION, "IAD ${rawIad.toHex()}")
            val counter = values.getValue(EmvTags.ATC).toUnsignedInt().also { atc = it }
            val requested = Bcd.decodeNumber(values.getValue(EmvTags.AMOUNT_AUTHORISED)).also { amount = it }

            if (!credential.accepts(iad.version)) return decline(DeclineReason.WRONG_KEY_TYPE, iad.version.label)
            val key = credential.cryptogramKey(iad, counter) { title, detail -> step(title, detail, passed = true) }

            val data = Dol.build(cdol1) { values[it] }
            val expected = CardCryptogram.compute(key, data, values.getValue(EmvTags.AIP), counter, iad.cvr, iad.version)
            val received = values.getValue(EmvTags.APPLICATION_CRYPTOGRAM)
            if (!expected.contentEquals(received)) {
                return decline(DeclineReason.CRYPTOGRAM_MISMATCH, "Expected ${expected.toHex()}, got ${received.toHex()}")
            }
            step("ARQC", "${received.toHex()} matches the recomputed MAC", passed = true)

            val lastAtc = credential.lastAtc
            if (counter <= lastAtc) {
                return decline(DeclineReason.REPLAYED_COUNTER, "ATC $counter, last seen $lastAtc")
            }
            step("Replay check", "ATC $counter is newer than ${lastAtc.takeIf { it >= 0 } ?: "none"}", passed = true)
            val counted = credential.counted(counter)
            accounts[key(account.pan, account.panSequenceNumber)] = counted

            val reason = cardDecision(account, values) ?: checkPin(credential) ?: fundsDecision(account, requested)
            val responseCode = reason?.responseCode ?: ResponseCode.APPROVED
            if (reason == null) {
                accounts[key(account.pan, account.panSequenceNumber)] = counted.copy(balance = account.balance - requested)
                step("Funds", "${Money.format(requested)} of ${Money.format(account.balance)} available", passed = true)
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

        private fun resolveCredential(): IssuerCredential {
            val service = tokens
            if (service == null || !service.vault.isToken(request.pan)) {
                val account = account(request.pan, request.panSequenceNumber)
                    ?: throw AuthorizationDecline(DeclineReason.UNKNOWN_ACCOUNT, SensitiveData.maskPan(request.pan))
                return CardCredential(account, issuerMasterKey)
            }
            val token = when (val result = service.vault.detokenize(request.pan, TokenDomain.CONTACTLESS, request.date)) {
                is DetokenizeResult.Refused -> throw AuthorizationDecline(DeclineReason.TOKEN_REFUSED, result.reason.description)
                is DetokenizeResult.Found -> result.record
            }
            step(
                "Token",
                "${SensitiveData.maskPan(token.token)} -> ${SensitiveData.maskPan(token.pan)} - PAR ${token.paymentAccountReference}",
                passed = true,
            )
            val account = account(token.pan, token.panSequenceNumber)
                ?: throw AuthorizationDecline(DeclineReason.UNKNOWN_ACCOUNT, SensitiveData.maskPan(request.pan))
            return TokenCredential(account, token, service, ledger)
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

        private fun checkPin(credential: IssuerCredential): DeclineReason? {
            val encrypted = request.onlinePin ?: return null
            val verdict = pins?.verify(credential.account.pan, encrypted, pinBlockPan = credential.pinBlockPan) ?: return DeclineReason.UNREADABLE_PIN
            val detail = "KSN ${encrypted.ksn} - ${verdict.check?.description ?: "PIN blocked"} - tries left ${verdict.triesLeft}"
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
