package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.DemoCards

object DemoIssuer {

    const val OPENING_BALANCE = 250_00L

    fun account(): IssuerAccount = IssuerAccount(
        pan = DemoCards.lab.pan,
        panSequenceNumber = DemoCards.lab.panSequenceNumber,
        expiry = DemoCards.lab.expiry,
        balance = OPENING_BALANCE,
    )

    fun create(): IssuerHost = IssuerHost(DemoCards.issuerMasterKey, listOf(account()))
}
