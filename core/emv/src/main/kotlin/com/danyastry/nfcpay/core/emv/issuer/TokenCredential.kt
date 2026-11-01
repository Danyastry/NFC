package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.IssuerApplicationData
import com.danyastry.nfcpay.core.emv.crypto.KeyCheckValue
import com.danyastry.nfcpay.core.emv.token.TokenRecord
import com.danyastry.nfcpay.core.emv.token.TokenService

class TokenCredential(
    override val account: IssuerAccount,
    private val token: TokenRecord,
    private val service: TokenService,
    private val ledger: TokenLedger,
) : IssuerCredential {

    private var keyIndex: Int? = null

    override val pinBlockPan: String
        get() = token.token

    override val lastAtc: Int
        get() = ledger.lastAtc(token.token)

    override fun accepts(version: CryptogramVersion): Boolean = version == CryptogramVersion.LIMITED_USE_KEY

    override fun cryptogramKey(iad: IssuerApplicationData, atc: Int, step: (title: String, detail: String) -> Unit): ByteArray {
        val index = iad.derivationKeyIndex
        val key = service.keyFor(token.token, index)
            ?: throw AuthorizationDecline(DeclineReason.KEY_RETIRED, "Key index $index, active ${service.activeKeyIndex(token.token)}")
        val used = ledger.uses(token.token, index)
        if (used >= service.usesPerKey) {
            throw AuthorizationDecline(DeclineReason.KEY_EXHAUSTED, "Key index $index already signed $used of ${service.usesPerKey} taps")
        }
        keyIndex = index
        step("Limited-use key", "${iad.version.label} - index $index - tap ${used + 1} of ${service.usesPerKey} - KCV ${KeyCheckValue.of(key)}")
        return key
    }

    override fun counted(atc: Int): IssuerAccount {
        val index = checkNotNull(keyIndex) { "The cryptogram key was never looked up" }
        ledger.record(token.token, atc, index)
        return account
    }
}
