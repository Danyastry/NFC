package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.hsm.DemoHsm
import com.danyastry.nfcpay.core.emv.issuer.AuthorizationResponse
import com.danyastry.nfcpay.core.emv.issuer.DeclineReason
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.valueOf
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class OnlinePinTest {

    private val date = LocalDate.of(2026, 10, 25)
    private val card = VirtualCard(DemoCards.lab)
    private val issuer = DemoIssuer.create(pins = DemoHsm.pinService())
    private val device = DemoHsm.pinPadDevice()
    private val responses = mutableListOf<AuthorizationResponse>()
    private var typed: String? = DemoHsm.CARD_PIN
    private var asked = 0
    private val pinPad = PinPad { pan, _ ->
        asked++
        typed?.let { device.encryptPin(it, pan) }
    }

    @Test
    fun `asks for a PIN above the contactless limit and approves the right one`() = runTest {
        val outcome = tap(amount = 60_00) as TransactionOutcome.Completed

        assertThat(asked).isEqualTo(1)
        assertThat(outcome.online!!.isApproved).isTrue()
        assertThat(outcome.online!!.finalCryptogram!!.type).isEqualTo(CryptogramType.TC)
        assertThat(outcome.tvr.bits).contains(TvrBit.ONLINE_PIN_ENTERED)
        assertThat(BerTlv.parse(outcome.field55.hexToBytes()).valueOf(EmvTags.CVM_RESULTS)!!.toHex()).isEqualTo("420300")
        assertThat(responses.single().steps.first { it.title == "Online PIN" }.passed).isTrue()
    }

    @Test
    fun `small amounts need no PIN`() = runTest {
        val outcome = tap(amount = 10_00) as TransactionOutcome.Completed

        assertThat(asked).isEqualTo(0)
        assertThat(BerTlv.parse(outcome.field55.hexToBytes()).valueOf(EmvTags.CVM_RESULTS)!!.toHex()).isEqualTo("3F0000")
        assertThat(outcome.online!!.isApproved).isTrue()
    }

    @Test
    fun `a wrong PIN is declined with 55 and the card ends with an AAC`() = runTest {
        typed = "9999"

        val online = (tap(amount = 60_00) as TransactionOutcome.Completed).online!!

        assertThat(online.responseCode).isEqualTo("55")
        assertThat(online.finalCryptogram!!.type).isEqualTo(CryptogramType.AAC)
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE)
    }

    @Test
    fun `three wrong PINs block it until the bank unblocks`() = runTest {
        typed = "9999"
        repeat(2) { tap(amount = 60_00) }
        val third = (tap(amount = 60_00) as TransactionOutcome.Completed).online!!
        typed = DemoHsm.CARD_PIN
        val afterBlock = (tap(amount = 60_00) as TransactionOutcome.Completed).online!!

        assertThat(responses.map { it.declineReason }).containsExactly(
            DeclineReason.INCORRECT_PIN, DeclineReason.INCORRECT_PIN, DeclineReason.PIN_TRIES_EXCEEDED, DeclineReason.PIN_TRIES_EXCEEDED,
        ).inOrder()
        assertThat(third.responseCode).isEqualTo("75")
        assertThat(afterBlock.responseCode).isEqualTo("75")

        issuer.reset(listOf(DemoIssuer.account()))
        assertThat((tap(amount = 60_00) as TransactionOutcome.Completed).online!!.isApproved).isTrue()
    }

    @Test
    fun `a wrong PIN says nothing about the balance`() = runTest {
        typed = "9999"

        tap(amount = DemoIssuer.OPENING_BALANCE + 1)

        assertThat(responses.single().declineReason).isEqualTo(DeclineReason.INCORRECT_PIN)
    }

    @Test
    fun `a cancelled PIN entry stops the tap before the cryptogram`() = runTest {
        typed = null

        val result = TerminalKernel(authorizer = IssuerAuthorizer(issuer), pinPad = pinPad)
            .run(LoopbackTransport(card), TransactionRequest(amount = 60_00, date = date))

        assertThat(result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.PIN_ENTRY_CANCELLED))
        assertThat(result.exchanges.map { it.label }).doesNotContain("GENERATE AC - ARQC")
    }

    private suspend fun tap(amount: Long): TransactionOutcome =
        TerminalKernel(authorizer = IssuerAuthorizer(issuer) { _, response -> responses += response }, pinPad = pinPad)
            .run(LoopbackTransport(card), TransactionRequest(amount = amount, date = date))
            .outcome
}
