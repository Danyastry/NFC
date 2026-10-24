package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer

object SharedIssuer {
    val link: IssuerLink = IssuerLink(DemoIssuer.create(), DemoIssuer.account())
}
