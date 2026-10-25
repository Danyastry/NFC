package com.danyastry.nfcpay.feature.terminal.ui

import com.danyastry.nfcpay.feature.terminal.logic.AmountInput
import com.danyastry.nfcpay.feature.terminal.model.CardSource
import com.danyastry.nfcpay.feature.terminal.model.TerminalPhase
import com.danyastry.nfcpay.feature.terminal.state.CompletedTap
import com.danyastry.nfcpay.feature.terminal.state.PinPrompt

data class TerminalUiState(
    val source: CardSource = CardSource.SIMULATED,
    val amountText: String = DEFAULT_AMOUNT,
    val phase: TerminalPhase = TerminalPhase.IDLE,
    val lastTap: CompletedTap? = null,
    val pinPrompt: PinPrompt? = null,
) {
    val amount: Long?
        get() = AmountInput.parse(amountText)

    val canTap: Boolean
        get() = amount != null && phase == TerminalPhase.IDLE

    companion object {
        const val DEFAULT_AMOUNT = "12.50"
    }
}
