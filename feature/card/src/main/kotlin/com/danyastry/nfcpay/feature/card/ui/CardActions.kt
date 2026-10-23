package com.danyastry.nfcpay.feature.card.ui

data class CardActions(
    val onResetCounter: () -> Unit,
    val onClearLog: () -> Unit,
    val onMakeDefault: () -> Unit,
)
