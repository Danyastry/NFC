package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CardFiles
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.crypto.Arpc
import com.danyastry.nfcpay.core.emv.crypto.IccKeyDerivation
import com.danyastry.nfcpay.core.emv.crypto.KeyCheckValue
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.dol.DolEntry
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException
import com.danyastry.nfcpay.core.emv.util.Bcd
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import java.time.YearMonth

class IssuerHost(
    private val issuerMasterKey: ByteArray,
    accounts: List<IssuerAccount>,
    private val cdol1: List<DolEntry> = CardFiles.CDOL1,
) {
    private val accounts = accounts.associateBy { key(it.pan, it.panSequenceNumber) }.toMutableMap()

    @Synchronized
    fun account(pan: String, panSequenceNumber: Int): IssuerAccount? = accounts[key(pan, panSequenceNumber)]

    @Synchronized
    fun reset(accounts: List<IssuerAccount>) {
        this.accounts.clear()
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

            val account = account(request.pan, request.panSequenceNumber)
                ?: return decline(DeclineReason.UNKNOWN_ACCOUNT, SensitiveData.maskPan(request.pan))
            step("Account", "${SensitiveData.maskPan(account.pan)} · sequence ${account.panSequenceNumber}", passed = true)

            val iad = values.getValue(EmvTags.ISSUER_APPLICATION_DATA)
            val version = iad.takeIf { it.size >= CardCryptogram.IAD_LENGTH }?.let { CryptogramVersion.of(it[CardCryptogram.CVN_OFFSET].toInt() and 0xFF) }
                ?: return decline(DeclineReason.UNKNOWN_CRYPTOGRAM_VERSION, "IAD ${iad.toHex()}")
            val counter = values.getValue(EmvTags.ATC).toUnsignedInt().also { atc = it }
            val requested = Bcd.decodeNumber(values.getValue(EmvTags.AMOUNT_AUTHORISED)).also { amount = it }

            val cardKey = IccKeyDerivation.optionA(issuerMasterKey, account.pan, account.panSequenceNumber)
            step("Card key", "Option A from the issuer master key · KCV ${KeyCheckValue.of(cardKey)}", passed = true)
            val key = CardCryptogram.key(cardKey, version, counter)
            step("Cryptogram key", "${version.label} · ATC $counter · KCV ${KeyCheckValue.of(key)}", passed = true)

            val cvr = iad.copyOfRange(CardCryptogram.CVR_OFFSET, CardCryptogram.IAD_LENGTH)
            val data = Dol.build(cdol1) { values[it] }
            val expected = CardCryptogram.compute(key, data, values.getValue(EmvTags.AIP), counter, cvr)
            val received = values.getValue(EmvTags.APPLICATION_CRYPTOGRAM)
            if (!expected.contentEquals(received)) {
                return decline(DeclineReason.CRYPTOGRAM_MISMATCH, "Expected ${expected.toHex()}, got ${received.toHex()}")
            }
            step("ARQC", "${received.toHex()} matches the recomputed MAC", passed = true)

            if (counter <= account.lastAtc) {
                return decline(DeclineReason.REPLAYED_COUNTER, "ATC $counter, last seen ${account.lastAtc}")
            }
            step("Replay check", "ATC $counter is newer than ${account.lastAtc.takeIf { it >= 0 } ?: "none"}", passed = true)
            accounts[key(account.pan, account.panSequenceNumber)] = account.copy(lastAtc = counter)

            val reason = businessDecision(account, values, requested)
            val responseCode = reason?.responseCode ?: ResponseCode.APPROVED
            if (reason == null) {
                accounts[key(account.pan, account.panSequenceNumber)] = account.copy(lastAtc = counter, balance = account.balance - requested)
                step("Funds", "$requested of ${account.balance} available", passed = true)
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

        private fun businessDecision(account: IssuerAccount, values: Map<Tag, ByteArray>, requested: Long): DeclineReason? {
            val cid = values.getValue(EmvTags.CRYPTOGRAM_INFORMATION_DATA).firstOrNull()?.toInt()?.and(0xFF) ?: 0
            return when {
                CryptogramType.fromBits(cid) == CryptogramType.AAC -> DeclineReason.CARD_DECLINED
                account.expiry.isBefore(YearMonth.from(request.date)) -> DeclineReason.EXPIRED_CARD
                requested > account.balance -> DeclineReason.INSUFFICIENT_FUNDS
                else -> null
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
