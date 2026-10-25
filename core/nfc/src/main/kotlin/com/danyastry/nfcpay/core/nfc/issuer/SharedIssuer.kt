package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.nfc.wallet.SharedWallet

object SharedIssuer {
    val link: IssuerLink = IssuerLink(DemoIssuer.create(SharedWallet.tokens), DemoIssuer.account())
}
