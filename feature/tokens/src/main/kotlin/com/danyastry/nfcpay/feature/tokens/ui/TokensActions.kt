package com.danyastry.nfcpay.feature.tokens.ui

data class TokensActions(
    val onAddCard: () -> Unit,
    val onReplenish: () -> Unit,
    val onSuspend: () -> Unit,
    val onResume: () -> Unit,
    val onRemove: () -> Unit,
)
