package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.transport.Exchange

data class TransactionResult(
    val exchanges: List<Exchange>,
    val outcome: TransactionOutcome,
)
