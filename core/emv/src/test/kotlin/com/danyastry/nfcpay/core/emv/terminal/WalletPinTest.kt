package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.hsm.DemoHsm
import com.danyastry.nfcpay.core.emv.issuer.AuthorizationResponse
import com.danyastry.nfcpay.core.emv.issuer.DeclineReason
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.danyastry.nfcpay.core.emv.token.WalletKeys
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class WalletPinTest {

    private val date = LocalDate.of(2026, 11, 1)
    private val tokens = TokenService(TokenVault(random = Random(5)), DemoCards.issuerMasterKey, usesPerKey = 10)
    private val pins = DemoHsm.pinService()
    private val issuer = DemoIssuer.create(tokens, pins)
    private val wallet = tokens.provisionWallet(DemoCards.lab.pan, 1, DemoCards.walletRequestorId, YearMonth.of(2029, 10))
    private val phone = VirtualCard(DemoCards.tokenCard(wallet.token), keys = WalletKeys(wallet.firstKey))
    private val device = DemoHsm.pinPadDevice()
    private val responses = mutableListOf<AuthorizationResponse>()
    private val boundPans = mutableListOf<String>()
    private var typed = DemoHsm.CARD_PIN
    private val pinPad = PinPad { pan, _ ->
        boundPans += pan
        device.encryptPin(typed, pan)
    }

    @Test
    fun `a wallet tap with the account PIN is approved`() = runTest {
        val online = tap(phone)

        assertThat(boundPans.single()).isEqualTo(wallet.token.token)
        assertThat(online.isApproved).isTrue()
        assertThat(responses.single().steps.first { it.title == "Online PIN" }.passed).isTrue()
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - AMOUNT)
    }

    @Test
    fun `a wrong PIN on the wallet counts against the account PIN`() = runTest {
        typed = "9999"

        val online = tap(phone)

        assertThat(online.responseCode).isEqualTo("55")
        assertThat(responses.single().declineReason).isEqualTo(DeclineReason.INCORRECT_PIN)
        assertThat(pins.record(DemoCards.lab.pan)!!.triesLeft).isEqualTo(2)
        assertThat(pins.record(wallet.token.token)).isNull()
    }

    private suspend fun tap(card: VirtualCard): OnlineResult {
        val kernel = TerminalKernel(authorizer = IssuerAuthorizer(issuer) { _, response -> responses += response }, pinPad = pinPad)
        val outcome = kernel.run(LoopbackTransport(card), TransactionRequest(amount = AMOUNT, date = date)).outcome as TransactionOutcome.Completed
        return outcome.online!!
    }

    private companion object {
        const val AMOUNT = 60_00L
    }
}
