package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.IssuerApplicationData
import com.danyastry.nfcpay.core.emv.crypto.IccKeyDerivation
import com.danyastry.nfcpay.core.emv.crypto.KeyCheckValue

class CardCredential(
    override val account: IssuerAccount,
    private val issuerMasterKey: ByteArray,
) : IssuerCredential {

    override val pinBlockPan: String
        get() = account.pan

    override val lastAtc: Int
        get() = account.lastAtc

    override fun accepts(version: CryptogramVersion): Boolean = version != CryptogramVersion.LIMITED_USE_KEY

    override fun cryptogramKey(iad: IssuerApplicationData, atc: Int, step: (title: String, detail: String) -> Unit): ByteArray {
        val cardKey = IccKeyDerivation.optionA(issuerMasterKey, account.pan, account.panSequenceNumber)
        step("Card key", "Option A from the issuer master key - KCV ${KeyCheckValue.of(cardKey)}")
        return CardCryptogram.key(cardKey, iad.version, atc).also {
            step("Cryptogram key", "${iad.version.label} - ATC $atc - KCV ${KeyCheckValue.of(it)}")
        }
    }

    override fun counted(atc: Int): IssuerAccount = account.copy(lastAtc = atc)
}
