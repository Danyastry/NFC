package com.danyastry.nfcpay.core.emv.terminal

import java.time.YearMonth

data class CardSummary(
    val pan: String,
    val expiry: YearMonth?,
    val label: String,
    val aid: String,
    val scheme: PaymentScheme?,
    val panSequenceNumber: Int?,
)
