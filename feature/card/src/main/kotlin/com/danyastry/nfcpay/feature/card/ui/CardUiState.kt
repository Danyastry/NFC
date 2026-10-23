package com.danyastry.nfcpay.feature.card.ui

import com.danyastry.nfcpay.feature.card.model.CardDetails
import com.danyastry.nfcpay.feature.card.model.ServedEntry

data class CardUiState(
    val details: CardDetails,
    val atc: Int = 0,
    val log: List<ServedEntry> = emptyList(),
)
