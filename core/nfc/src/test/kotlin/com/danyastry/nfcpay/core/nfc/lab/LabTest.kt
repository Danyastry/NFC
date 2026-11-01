package com.danyastry.nfcpay.core.nfc.lab

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.hsm.DemoHsm
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class LabTest {

    private val lab = Lab()
    private val date = LocalDate.of(2026, 11, 1)
    private var asked = 0
    private val pinPad = lab.pinPad { _, _ ->
        asked++
        DemoHsm.CARD_PIN
    }

    @Test
    fun `the virtual card asks for a PIN above the limit and the issuer verifies it`() = runTest {
        val outcome = tap(CardSource.SIMULATED, amount = 60_00)

        assertThat(asked).isEqualTo(1)
        assertThat(outcome.online!!.isApproved).isTrue()
        assertThat(lab.issuer.events.value.last().response.steps.single { it.title == "Online PIN" }.passed).isTrue()
    }

    @Test
    fun `the wallet goes online without a PIN pad`() = runTest {
        assertThat(lab.loopbackCard(CardSource.WALLET)).isNull()
        lab.wallet.addCard()

        val outcome = tap(CardSource.WALLET, amount = 60_00)

        assertThat(asked).isEqualTo(0)
        assertThat(outcome.online!!.isApproved).isTrue()
        assertThat(outcome.card.pan).isEqualTo(lab.wallet.state.value!!.token.token)
    }

    @Test
    fun `a real card never goes online`() = runTest {
        assertThat(lab.loopbackCard(CardSource.NFC)).isNull()

        val result = lab.terminal(CardSource.NFC, pinPad).run(LoopbackTransport(VirtualCard(DemoCards.lab)), TransactionRequest(amount = 60_00, date = date))

        assertThat((result.outcome as TransactionOutcome.Completed).online).isNull()
        assertThat(asked).isEqualTo(0)
    }

    @Test
    fun `each lab is a separate world`() = runTest {
        tap(CardSource.SIMULATED, amount = 10_00)

        val fresh = Lab()

        assertThat(fresh.card.atc.value).isEqualTo(0)
        assertThat(fresh.issuer.events.value).isEmpty()
        assertThat(lab.card.atc.value).isEqualTo(1)
    }

    private suspend fun tap(source: CardSource, amount: Long): TransactionOutcome.Completed {
        val card = requireNotNull(lab.loopbackCard(source))
        val result = lab.terminal(source, pinPad).run(LoopbackTransport(card), TransactionRequest(amount = amount, date = date))
        return result.outcome as TransactionOutcome.Completed
    }
}
