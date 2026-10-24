package com.danyastry.nfcpay.feature.issuer.ui

data class IssuerActions(
    val onReplay: () -> Unit,
    val onReset: () -> Unit,
)
