package com.danyastry.nfcpay.feature.terminal.ui

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.hsm.DemoHsm
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.danyastry.nfcpay.core.nfc.wallet.PhoneWallet
import com.danyastry.nfcpay.core.emv.terminal.TerminalError
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.feature.terminal.model.CardSource
import com.danyastry.nfcpay.feature.terminal.model.TerminalPhase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    private val card = VirtualCard(DemoCards.lab)
    private lateinit var viewModel: TerminalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TerminalViewModel(card, today = { LocalDate.of(2026, 10, 23) })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a simulated tap runs the kernel against the virtual card`() = runTest {
        viewModel.onAmountChange("7.25")
        viewModel.onTap()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.phase).isEqualTo(TerminalPhase.IDLE)
        val tap = state.lastTap!!
        assertThat(tap.amount).isEqualTo(725)
        assertThat(tap.maskCardData).isFalse()
        assertThat((tap.result.outcome as TransactionOutcome.Completed).decision).isEqualTo(CryptogramType.ARQC)
        assertThat(card.atc).isEqualTo(1)
    }

    @Test
    fun `the virtual card goes online and real cards do not`() = runTest {
        val issuer = DemoIssuer.create()
        val online = TerminalViewModel(card, IssuerAuthorizer(issuer), today = { LocalDate.of(2026, 10, 23) })

        online.onTap()
        advanceUntilIdle()
        val simulated = online.state.value.lastTap!!.result.outcome as TransactionOutcome.Completed
        assertThat(simulated.online!!.isApproved).isTrue()

        online.onSourceChange(CardSource.NFC)
        online.onTap()
        online.onCardPresented { CardTransport { card.process(it) } }
        advanceUntilIdle()
        val real = online.state.value.lastTap!!.result.outcome as TransactionOutcome.Completed
        assertThat(real.online).isNull()
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.lastAtc).isEqualTo(1)
    }

    @Test
    fun `the phone wallet pays with its token`() = runTest {
        val tokens = TokenService(TokenVault(), DemoCards.issuerMasterKey)
        val wallet = PhoneWallet(tokens)
        val viewModel = TerminalViewModel(card, IssuerAuthorizer(DemoIssuer.create(tokens)), walletCard = wallet::handler, today = { LocalDate.of(2026, 10, 25) })
        viewModel.onSourceChange(CardSource.WALLET)

        viewModel.onTap()
        assertThat(viewModel.state.value.lastTap).isNull()

        wallet.addCard()
        viewModel.onTap()
        advanceUntilIdle()

        val outcome = viewModel.state.value.lastTap!!.result.outcome as TransactionOutcome.Completed
        assertThat(outcome.card.pan).isEqualTo(wallet.state.value!!.token.token)
        assertThat(outcome.online!!.isApproved).isTrue()
        assertThat(viewModel.state.value.lastTap!!.maskCardData).isFalse()
    }

    @Test
    fun `asks for a PIN above the limit and sends it encrypted`() = runTest {
        val issuer = DemoIssuer.create(pins = DemoHsm.pinService())
        val device = DemoHsm.pinPadDevice()
        val viewModel = TerminalViewModel(card, IssuerAuthorizer(issuer), encryptPin = device::encryptPin, today = { LocalDate.of(2026, 10, 25) })
        viewModel.onAmountChange("60.00")

        viewModel.onTap()
        advanceUntilIdle()
        val prompt = viewModel.state.value.pinPrompt!!
        assertThat(prompt.amount).isEqualTo("60.00")
        assertThat(prompt.maskedPan).isEqualTo("999900******7891")

        viewModel.onPinSubmit(DemoHsm.CARD_PIN)
        advanceUntilIdle()

        val outcome = viewModel.state.value.lastTap!!.result.outcome as TransactionOutcome.Completed
        assertThat(viewModel.state.value.pinPrompt).isNull()
        assertThat(outcome.online!!.isApproved).isTrue()
        assertThat(device.nextCounter).isEqualTo(2)
    }

    @Test
    fun `cancelling the PIN pad fails the tap`() = runTest {
        val viewModel = TerminalViewModel(card, IssuerAuthorizer(DemoIssuer.create(pins = DemoHsm.pinService())), encryptPin = DemoHsm.pinPadDevice()::encryptPin)
        viewModel.onAmountChange("60.00")

        viewModel.onTap()
        advanceUntilIdle()
        viewModel.onPinCancel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.lastTap!!.result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.PIN_ENTRY_CANCELLED))
        assertThat(viewModel.state.value.phase).isEqualTo(TerminalPhase.IDLE)
    }

    @Test
    fun `an invalid amount blocks the tap`() = runTest {
        viewModel.onAmountChange("1.234")
        viewModel.onTap()

        assertThat(viewModel.state.value.canTap).isFalse()
        assertThat(viewModel.state.value.lastTap).isNull()
        assertThat(card.atc).isEqualTo(0)
    }

    @Test
    fun `a real card tap waits for the card and masks its data`() = runTest {
        viewModel.onSourceChange(CardSource.NFC)
        viewModel.onTap()
        assertThat(viewModel.state.value.phase).isEqualTo(TerminalPhase.WAITING_FOR_CARD)

        var closed = false
        viewModel.onCardPresented {
            object : CardTransport, Closeable {
                override suspend fun transceive(command: ByteArray) = card.process(command)
                override fun close() {
                    closed = true
                }
            }
        }
        advanceUntilIdle()

        val tap = viewModel.state.value.lastTap!!
        assertThat(tap.source).isEqualTo(CardSource.NFC)
        assertThat(tap.maskCardData).isTrue()
        assertThat(tap.result.outcome).isInstanceOf(TransactionOutcome.Completed::class.java)
        assertThat(closed).isTrue()
    }

    @Test
    fun `cards presented while not waiting are ignored`() = runTest {
        var opened = false
        viewModel.onCardPresented {
            opened = true
            CardTransport { card.process(it) }
        }

        assertThat(opened).isFalse()
        assertThat(viewModel.state.value.lastTap).isNull()
    }

    @Test
    fun `a card that cannot be opened ends as lost`() = runTest {
        viewModel.onSourceChange(CardSource.NFC)
        viewModel.onTap()
        viewModel.onCardPresented { throw IOException("Tag was lost") }
        advanceUntilIdle()

        assertThat(viewModel.state.value.lastTap!!.result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.CARD_LOST))
        assertThat(viewModel.state.value.phase).isEqualTo(TerminalPhase.IDLE)
    }

    @Test
    fun `cancel stops waiting`() = runTest {
        viewModel.onSourceChange(CardSource.NFC)
        viewModel.onTap()
        viewModel.onCancel()

        assertThat(viewModel.state.value.phase).isEqualTo(TerminalPhase.IDLE)
    }
}
