package com.danyastry.nfcpay.core.nfc.hce

import com.danyastry.nfcpay.core.emv.card.DemoCards

object SharedCard {
    val host: CardHost = CardHost(DemoCards.lab)
}
