package com.danyastry.nfcpay.feature.terminal.state

data class PinPrompt(
    val maskedPan: String,
    val amount: String,
)
