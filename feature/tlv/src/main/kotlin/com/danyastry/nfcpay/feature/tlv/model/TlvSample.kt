package com.danyastry.nfcpay.feature.tlv.model

import com.danyastry.nfcpay.core.emv.card.CardFiles
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.util.toHex

enum class TlvSample(val label: String, val hex: () -> String) {
    PPSE("PPSE", { CardFiles(DemoCards.lab).ppse().toHex() }),
    SELECT("SELECT", { CardFiles(DemoCards.lab).applicationFci().toHex() }),
    RECORD("Record", { CardFiles(DemoCards.lab).record(CardFiles.SFI, 2)!!.toHex() }),
}
