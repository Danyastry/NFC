package com.danyastry.nfcpay.core.nfc.wallet

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenStatus
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class PhoneWalletTest {

    private val service = TokenService(TokenVault(random = Random(9)), DemoCards.issuerMasterKey, usesPerKey = 2)
    private val wallet = PhoneWallet(service, tokenExpiry = { YearMonth.of(2029, 10) })
    private val kernel = TerminalKernel(authorizer = IssuerAuthorizer(DemoIssuer.create(service)))

    @Test
    fun `starts empty and adds the card once`() {
        assertThat(wallet.state.value).isNull()
        assertThat(wallet.handler()).isNull()

        wallet.addCard()
        wallet.addCard()

        val state = wallet.state.value!!
        assertThat(state.token.pan).isEqualTo(DemoCards.lab.pan)
        assertThat(state.keyIndex).isEqualTo(1)
        assertThat(state.tapsLeft).isEqualTo(2)
        assertThat(wallet.tokens.value).hasSize(1)
    }

    @Test
    fun `taps use up the key and new keys bring them back`() = runTest {
        wallet.addCard()

        repeat(2) { assertThat(pay()).isTrue() }
        assertThat(wallet.state.value!!.tapsLeft).isEqualTo(0)
        assertThat(wallet.state.value!!.atc).isEqualTo(2)
        assertThat(pay()).isFalse()

        wallet.replenish()

        assertThat(wallet.state.value!!.keyIndex).isEqualTo(2)
        assertThat(pay()).isTrue()
    }

    @Test
    fun `a suspended token is declined until it is resumed`() = runTest {
        wallet.addCard()

        wallet.suspend()
        assertThat(wallet.state.value!!.token.status).isEqualTo(TokenStatus.SUSPENDED)
        assertThat(pay()).isFalse()

        wallet.resume()
        assertThat(pay()).isTrue()
    }

    @Test
    fun `removing the card deletes the token and a new one can be added`() {
        wallet.addCard()
        val first = wallet.state.value!!.token.token

        wallet.remove()
        assertThat(wallet.state.value).isNull()
        wallet.addCard()

        assertThat(wallet.state.value!!.token.token).isNotEqualTo(first)
        assertThat(wallet.tokens.value.map { it.status }).containsExactly(TokenStatus.DELETED, TokenStatus.ACTIVE).inOrder()
    }

    private suspend fun pay(): Boolean {
        val result = kernel.run(LoopbackTransport(wallet.handler()!!), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 25)))
        return (result.outcome as? TransactionOutcome.Completed)?.online?.isApproved == true
    }
}
