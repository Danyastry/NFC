package com.danyastry.nfcpay.core.emv.terminal

data class TransactionResult(
    val exchanges: List<Exchange>,
    val outcome: TransactionOutcome,
)
