package com.danyastry.nfcpay.feature.terminal.ui

import com.danyastry.nfcpay.feature.terminal.model.CardSource

data class TerminalActions(
    val onSourceChange: (CardSource) -> Unit,
    val onAmountChange: (String) -> Unit,
    val onTap: () -> Unit,
    val onCancel: () -> Unit,
)
