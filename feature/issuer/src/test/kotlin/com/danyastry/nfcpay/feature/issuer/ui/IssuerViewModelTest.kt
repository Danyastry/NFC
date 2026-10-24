package com.danyastry.nfcpay.feature.issuer.ui

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.nfc.issuer.IssuerLink
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
class IssuerViewModelTest {

    private val link = IssuerLink(DemoIssuer.create(), DemoIssuer.account())
    private lateinit var viewModel: IssuerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = IssuerViewModel(link, DemoCards.issuerMasterKey, DemoIssuer.account())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows an approval with its steps and the new balance`() = runTest {
        tap(amount = 12_50)

        val state = viewModel.state.value
        val approval = state.authorizations.single()
        assertThat(approval.isApproved).isTrue()
        assertThat(approval.amount).isEqualTo("12.50")
        assertThat(approval.steps.map { it.title }).containsAtLeast("ARQC", "Replay check", "ARPC").inOrder()
        assertThat(state.account!!.balance).isEqualTo("237.50")
        assertThat(state.canReplay).isTrue()
    }

    @Test
    fun `a replay shows up first and is declined`() = runTest {
        tap(amount = 100)

        viewModel.onReplay()

        val newest = viewModel.state.value.authorizations.first()
        assertThat(newest.isReplay).isTrue()
        assertThat(newest.responseCode).isEqualTo("63")
        assertThat(newest.steps.last().passed).isFalse()
    }

    @Test
    fun `reset clears the log`() = runTest {
        tap(amount = 100)

        viewModel.onReset()

        assertThat(viewModel.state.value.authorizations).isEmpty()
        assertThat(viewModel.state.value.account!!.balance).isEqualTo("250.00")
    }

    private suspend fun tap(amount: Long) {
        TerminalKernel(authorizer = link.authorizer)
            .run(LoopbackTransport(VirtualCard(DemoCards.lab)), TransactionRequest(amount = amount, date = LocalDate.of(2026, 10, 24)))
    }
}
