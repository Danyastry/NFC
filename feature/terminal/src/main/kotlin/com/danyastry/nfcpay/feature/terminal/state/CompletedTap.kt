package com.danyastry.nfcpay.feature.terminal.state

import com.danyastry.nfcpay.core.emv.terminal.TransactionResult
import com.danyastry.nfcpay.core.nfc.lab.CardSource

data class CompletedTap(
    val source: CardSource,
    val amount: Long,
    val result: TransactionResult,
) {
    val maskCardData: Boolean
        get() = source == CardSource.NFC
}
