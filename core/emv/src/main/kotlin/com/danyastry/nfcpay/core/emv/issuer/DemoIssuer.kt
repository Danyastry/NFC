package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.hsm.PinService
import com.danyastry.nfcpay.core.emv.token.TokenService

object DemoIssuer {

    const val OPENING_BALANCE = 250_00L

    fun account(): IssuerAccount = IssuerAccount(
        pan = DemoCards.lab.pan,
        panSequenceNumber = DemoCards.lab.panSequenceNumber,
        expiry = DemoCards.lab.expiry,
        balance = OPENING_BALANCE,
    )

    fun create(tokens: TokenService? = null, pins: PinService? = null): IssuerHost =
        IssuerHost(DemoCards.issuerMasterKey, listOf(account()), tokens = tokens, pins = pins)
}
