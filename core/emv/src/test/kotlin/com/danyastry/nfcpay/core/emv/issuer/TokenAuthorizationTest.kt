package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardProfile
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.IssuerAuthentication
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.terminal.OnlineResult
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.token.LimitedUseKey
import com.danyastry.nfcpay.core.emv.token.TokenDomain
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.danyastry.nfcpay.core.emv.token.WalletKeys
import com.danyastry.nfcpay.core.emv.token.WalletProvisioning
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class TokenAuthorizationTest {

    private val date = LocalDate.of(2026, 10, 25)
    private val service = TokenService(TokenVault(random = Random(3)), DemoCards.issuerMasterKey, usesPerKey = 3)
    private val issuer = DemoIssuer.create(service)
    private val responses = mutableListOf<AuthorizationResponse>()
    private val wallet: WalletProvisioning = service.provisionWallet(DemoCards.lab.pan, 1, DemoCards.walletRequestorId, YearMonth.of(2029, 10))
    private val keys = WalletKeys(wallet.firstKey)
    private val phone = VirtualCard(DemoCards.tokenCard(wallet.token), keys = keys)

    @Test
    fun `a wallet tap is approved and charged to the real account`() = runTest {
        val online = tap(phone, amount = 20_00)

        assertThat(online.isApproved).isTrue()
        assertThat(online.finalCryptogram!!.type).isEqualTo(CryptogramType.TC)
        assertThat(phone.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.PASSED)
        val response = responses.single()
        assertThat(response.steps.map { it.title }).containsAtLeast("Token", "Account", "Limited-use key", "ARQC", "Replay check").inOrder()
        assertThat(response.steps.first { it.title == "Token" }.detail).contains(wallet.token.paymentAccountReference)
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - 20_00)
    }

    @Test
    fun `the phone needs fresh keys after three taps`() = runTest {
        repeat(3) { assertThat(tap(phone, amount = 100).isApproved).isTrue() }

        val fourth = TerminalKernel(authorizer = IssuerAuthorizer(issuer)).run(LoopbackTransport(phone), TransactionRequest(amount = 100, date = date))
        assertThat(fourth.outcome).isInstanceOf(TransactionOutcome.Failed::class.java)

        keys.replenish(service.replenish(wallet.token.token))
        assertThat(tap(phone, amount = 100).isApproved).isTrue()
        assertThat(responses.last().steps.first { it.title == "Limited-use key" }.detail).contains("index 2 · tap 1 of 3")
    }

    @Test
    fun `a wallet that ignores the limit is stopped by the issuer`() = runTest {
        val greedy = VirtualCard(DemoCards.tokenCard(wallet.token), keys = WalletKeys(LimitedUseKey(1, wallet.firstKey.key, usesLeft = 10)))

        repeat(3) { assertThat(tap(greedy, amount = 100).isApproved).isTrue() }
        val fourth = tap(greedy, amount = 100)

        assertThat(fourth.isApproved).isFalse()
        assertThat(responses.last().declineReason).isEqualTo(DeclineReason.KEY_EXHAUSTED)
    }

    @Test
    fun `an old key copied off the phone is useless after replenishment`() = runTest {
        val stolen = VirtualCard(DemoCards.tokenCard(wallet.token), initialAtc = 50, keys = WalletKeys(wallet.firstKey))
        keys.replenish(service.replenish(wallet.token.token))

        tap(stolen, amount = 100)

        assertThat(responses.last().declineReason).isEqualTo(DeclineReason.KEY_RETIRED)
        assertThat(responses.last().arpc).isNull()
    }

    @Test
    fun `a suspended token is refused before any crypto`() = runTest {
        service.vault.suspend(wallet.token.token)

        val online = tap(phone, amount = 100)

        assertThat(online.isApproved).isFalse()
        assertThat(responses.last().declineReason).isEqualTo(DeclineReason.TOKEN_REFUSED)
        assertThat(responses.last().responseCode.code).isEqualTo("62")
        assertThat(responses.last().arpc).isNull()
        assertThat(phone.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.NOT_PERFORMED)
    }

    @Test
    fun `a shop token cannot pay at a terminal`() = runTest {
        val shop = service.vault.provision(DemoCards.lab.pan, 1, "40010075001", TokenDomain.ECOMMERCE, YearMonth.of(2029, 10))
        val card = VirtualCard(DemoCards.tokenCard(shop), keys = WalletKeys(LimitedUseKey(1, service.tokenMasterKey(shop.token), 3)))

        tap(card, amount = 100)

        assertThat(responses.last().declineReason).isEqualTo(DeclineReason.TOKEN_REFUSED)
        assertThat(responses.last().steps.last().detail).isEqualTo("The token is not allowed in this channel")
    }

    @Test
    fun `token and plastic card keep separate counters`() = runTest {
        val plastic = VirtualCard(DemoCards.lab)

        assertThat(tap(plastic, amount = 100).isApproved).isTrue()
        assertThat(tap(phone, amount = 100).isApproved).isTrue()

        assertThat(responses.map { it.atc }).containsExactly(1, 1).inOrder()
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.lastAtc).isEqualTo(1)
    }

    @Test
    fun `a token must sign with a limited-use key`() = runTest {
        val wrong = CardProfile(
            aid = DemoCards.lab.aid, label = "TOKEN", pan = wallet.token.token, expiry = wallet.token.expiry,
            effective = DemoCards.lab.effective, cardholderName = "TEST", panSequenceNumber = 0, issuerCountryCode = 840,
            currencyCode = 840, masterKey = service.tokenMasterKey(wallet.token.token), cryptogramVersion = CryptogramVersion.SESSION_KEY,
        )

        tap(VirtualCard(wrong), amount = 100)

        assertThat(responses.last().declineReason).isEqualTo(DeclineReason.WRONG_KEY_TYPE)
    }

    @Test
    fun `a replayed wallet tap is caught`() = runTest {
        var captured: AuthorizationRequest? = null
        TerminalKernel(authorizer = IssuerAuthorizer(issuer) { request, _ -> captured = request })
            .run(LoopbackTransport(phone), TransactionRequest(amount = 100, date = date))

        val replay = issuer.authorize(captured!!)

        assertThat(replay.declineReason).isEqualTo(DeclineReason.REPLAYED_COUNTER)
    }

    private suspend fun tap(card: VirtualCard, amount: Long): OnlineResult {
        val result = TerminalKernel(authorizer = IssuerAuthorizer(issuer) { _, response -> responses += response })
            .run(LoopbackTransport(card), TransactionRequest(amount = amount, date = date))
        return (result.outcome as TransactionOutcome.Completed).online!!
    }
}
