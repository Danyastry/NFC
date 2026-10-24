package com.danyastry.nfcpay.feature.terminal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.nfcpay.core.emv.card.ApduHandler
import com.danyastry.nfcpay.core.emv.terminal.OnlineAuthorizer
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.terminal.TransactionResult
import com.danyastry.nfcpay.core.emv.terminal.TerminalError
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.feature.terminal.model.CardSource
import com.danyastry.nfcpay.feature.terminal.model.TerminalPhase
import com.danyastry.nfcpay.feature.terminal.state.CompletedTap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.IOException
import java.time.LocalDate

class TerminalViewModel(
    private val simulatedCard: ApduHandler,
    private val authorizer: OnlineAuthorizer? = null,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _state = MutableStateFlow(TerminalUiState())
    val state: StateFlow<TerminalUiState> = _state.asStateFlow()

    fun onSourceChange(source: CardSource) {
        _state.update { if (it.phase == TerminalPhase.READING) it else it.copy(source = source, phase = TerminalPhase.IDLE) }
    }

    fun onAmountChange(text: String) {
        _state.update { it.copy(amountText = text) }
    }

    fun onTap() {
        val current = _state.value
        val amount = current.amount ?: return
        if (current.phase != TerminalPhase.IDLE) return
        when (current.source) {
            CardSource.SIMULATED -> run(CardSource.SIMULATED, amount, TerminalKernel(authorizer = authorizer)) { LoopbackTransport(simulatedCard) }
            CardSource.NFC -> _state.update { it.copy(phase = TerminalPhase.WAITING_FOR_CARD) }
        }
    }

    fun onCancel() {
        _state.update { if (it.phase == TerminalPhase.WAITING_FOR_CARD) it.copy(phase = TerminalPhase.IDLE) else it }
    }

    fun onCardPresented(open: suspend () -> CardTransport) {
        val current = _state.value
        if (current.phase != TerminalPhase.WAITING_FOR_CARD) return
        val amount = current.amount ?: return
        run(CardSource.NFC, amount, TerminalKernel(), open)
    }

    private fun run(source: CardSource, amount: Long, kernel: TerminalKernel, open: suspend () -> CardTransport) {
        _state.update { it.copy(phase = TerminalPhase.READING) }
        viewModelScope.launch {
            val result = try {
                val transport = open()
                try {
                    kernel.run(transport, TransactionRequest(amount = amount, date = today()))
                } finally {
                    (transport as? Closeable)?.close()
                }
            } catch (_: IOException) {
                TransactionResult(emptyList(), TransactionOutcome.Failed(TerminalError.CARD_LOST))
            }
            _state.update { it.copy(phase = TerminalPhase.IDLE, lastTap = CompletedTap(source, amount, result)) }
        }
    }
}
