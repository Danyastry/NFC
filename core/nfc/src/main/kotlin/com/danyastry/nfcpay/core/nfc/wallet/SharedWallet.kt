package com.danyastry.nfcpay.core.nfc.wallet

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault

object SharedWallet {
    val tokens: TokenService = TokenService(TokenVault(), DemoCards.issuerMasterKey)
    val wallet: PhoneWallet = PhoneWallet(tokens)
}
