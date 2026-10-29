package com.danyastry.nfcpay.feature.card.ui

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.nfc.hce.ApduSource
import com.danyastry.nfcpay.core.nfc.hce.CardHost
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CardViewModelTest {

    private val host = CardHost(DemoCards.lab)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the newest command first and follows the counter`() = runTest {
        val viewModel = CardViewModel(host)

        TerminalKernel().run(LoopbackTransport(host.handler(ApduSource.SIMULATED)), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 23)))

        val state = viewModel.state.value
        assertThat(state.atc).isEqualTo(1)
        assertThat(state.log.map { it.label }.first()).isEqualTo("GENERATE AC - ARQC")
        assertThat(state.log.map { it.label }.last()).isEqualTo("SELECT PPSE")
    }

    @Test
    fun `resets the counter and clears the log`() = runTest {
        val viewModel = CardViewModel(host)
        TerminalKernel().run(LoopbackTransport(host.handler(ApduSource.SIMULATED)), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 23)))

        viewModel.onResetCounter()
        viewModel.onClearLog()

        assertThat(viewModel.state.value.atc).isEqualTo(0)
        assertThat(viewModel.state.value.log).isEmpty()
    }
}
