package com.danyastry.nfcpay.core.emv.terminal

import java.time.LocalDate

class OnlineRequest(
    val card: CardSummary,
    val field55: ByteArray,
    val date: LocalDate,
)
