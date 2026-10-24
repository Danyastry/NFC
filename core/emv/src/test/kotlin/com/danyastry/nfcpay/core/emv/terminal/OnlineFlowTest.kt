package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.IssuerAuthentication
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.issuer.AuthorizationResponse
import com.danyastry.nfcpay.core.emv.issuer.DeclineReason
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class OnlineFlowTest {

    private val date = LocalDate.of(2026, 10, 24)
    private val card = VirtualCard(DemoCards.lab)
    private val issuer = DemoIssuer.create()
    private val responses = mutableListOf<AuthorizationResponse>()

    @Test
    fun `an approved tap ends with a TC after the card checks the ARPC`() = runTest {
        val result = run(amount = 12_50, authorizer = IssuerAuthorizer(issuer) { _, response -> responses += response })

        assertThat(result.exchanges.map { it.label }.takeLast(2)).containsExactly("GENERATE AC · ARQC", "GENERATE AC · TC").inOrder()
        val online = (result.outcome as TransactionOutcome.Completed).online!!
        assertThat(online.responseCode).isEqualTo("00")
        assertThat(online.reachedIssuer).isTrue()
        assertThat(online.finalCryptogram!!.type).isEqualTo(CryptogramType.TC)
        assertThat(online.isApproved).isTrue()
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.PASSED)
        assertThat(responses.single().isApproved).isTrue()
    }

    @Test
    fun `a decline from the issuer ends with an AAC`() = runTest {
        val result = run(amount = DemoIssuer.OPENING_BALANCE + 1, authorizer = IssuerAuthorizer(issuer) { _, response -> responses += response })

        val online = (result.outcome as TransactionOutcome.Completed).online!!
        assertThat(online.responseCode).isEqualTo("51")
        assertThat(online.finalCryptogram!!.type).isEqualTo(CryptogramType.AAC)
        assertThat(online.isApproved).isFalse()
        assertThat(responses.single().declineReason).isEqualTo(DeclineReason.INSUFFICIENT_FUNDS)
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.PASSED)
    }

    @Test
    fun `the card refuses an approval it cannot authenticate`() = runTest {
        val forger = OnlineAuthorizer { OnlineResponse("00", "01020304050607083030".hexToBytes()) }

        val online = (run(amount = 100, authorizer = forger).outcome as TransactionOutcome.Completed).online!!

        assertThat(online.finalCryptogram!!.type).isEqualTo(CryptogramType.AAC)
        assertThat(online.cardOverruledIssuer).isTrue()
        assertThat(online.isApproved).isFalse()
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.FAILED)
    }

    @Test
    fun `an unreachable issuer ends with an offline decline`() = runTest {
        val offline = OnlineAuthorizer { throw IOException("No network") }

        val online = (run(amount = 100, authorizer = offline).outcome as TransactionOutcome.Completed).online!!

        assertThat(online.responseCode).isEqualTo(OnlineResponse.UNABLE_TO_GO_ONLINE)
        assertThat(online.reachedIssuer).isFalse()
        assertThat(online.finalCryptogram!!.type).isEqualTo(CryptogramType.AAC)
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.NOT_PERFORMED)
    }

    @Test
    fun `without an authorizer the kernel stops at the ARQC`() = runTest {
        val outcome = run(amount = 100, authorizer = null).outcome as TransactionOutcome.Completed

        assertThat(outcome.online).isNull()
        assertThat(outcome.decision).isEqualTo(CryptogramType.ARQC)
    }

    @Test
    fun `replaying the same tap to the issuer fails the second time`() = runTest {
        var captured: OnlineRequest? = null
        val authorizer = IssuerAuthorizer(issuer) { _, response -> responses += response }
        run(amount = 100, authorizer = { request -> captured = request; authorizer.authorize(request) })

        authorizer.authorize(captured!!)

        assertThat(responses.map { it.declineReason }).containsExactly(null, DeclineReason.REPLAYED_COUNTER).inOrder()
    }

    private suspend fun run(amount: Long, authorizer: OnlineAuthorizer?): TransactionResult =
        TerminalKernel(authorizer = authorizer).run(LoopbackTransport(card), TransactionRequest(amount = amount, date = date))
}
