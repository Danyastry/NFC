package com.danyastry.nfcpay.feature.card.model

data class CardDetails(
    val label: String,
    val pan: String,
    val expiry: String,
    val cardholderName: String,
    val aid: String,
)
