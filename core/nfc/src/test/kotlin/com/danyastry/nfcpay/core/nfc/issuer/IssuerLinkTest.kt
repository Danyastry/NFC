package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.issuer.DeclineReason
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class IssuerLinkTest {

    private val link = IssuerLink(DemoIssuer.create(), DemoIssuer.account(), clock = { 0L }, logLimit = 3)
    private val card = VirtualCard(DemoCards.lab)
    private val kernel = TerminalKernel(authorizer = link.authorizer)

    @Test
    fun `records every answer and follows the balance`() = runTest {
        repeat(4) { tap() }

        assertThat(link.events.value.map { it.response.atc }).containsExactly(2, 3, 4).inOrder()
        assertThat(link.account.value!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - 400)
        assertThat(link.account.value!!.lastAtc).isEqualTo(4)
    }

    @Test
    fun `a replay of the last tap is caught`() = runTest {
        assertThat(link.replayLast()).isNull()
        tap()

        val replay = link.replayLast()!!

        assertThat(replay.declineReason).isEqualTo(DeclineReason.REPLAYED_COUNTER)
        assertThat(link.events.value.map { it.isReplay }).containsExactly(false, true).inOrder()
        assertThat(link.account.value!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - 100)
    }

    @Test
    fun `reset restores the opening account`() = runTest {
        tap()

        link.reset()

        assertThat(link.account.value).isEqualTo(DemoIssuer.account())
        assertThat(link.events.value).isEmpty()
        assertThat(link.canReplay.value).isFalse()
    }

    private suspend fun tap() {
        kernel.run(LoopbackTransport(card), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 24)))
    }
}
