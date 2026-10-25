package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.crypto.Arpc
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.tlv.encode
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.emv.util.Bcd
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class IssuerHostTest {

    private val date = LocalDate.of(2026, 10, 24)
    private val card = VirtualCard(DemoCards.lab)
    private val issuer = DemoIssuer.create()

    @Test
    fun `approves a genuine tap and answers with an ARPC`() = runTest {
        val field55 = tap(amount = 12_50)

        val response = issuer.authorize(request(field55))

        assertThat(response.responseCode).isEqualTo(ResponseCode.APPROVED)
        assertThat(response.declineReason).isNull()
        assertThat(response.amount).isEqualTo(12_50)
        assertThat(response.atc).isEqualTo(1)
        assertThat(response.steps.all { it.passed }).isTrue()
        assertThat(response.steps.first { it.title == "Funds" }.detail).isEqualTo("12.50 of 250.00 available")
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - 12_50)
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.lastAtc).isEqualTo(1)
    }

    @Test
    fun `the ARPC is method 1 under the same session key`() = runTest {
        val field55 = tap(amount = 100)
        val values = BerTlv.parse(field55).associate { it.tag to it.value }

        val response = issuer.authorize(request(field55))

        val sessionKey = CardCryptogram.key(DemoCards.lab.masterKey, CryptogramVersion.SESSION_KEY, atc = 1)
        val expected = Arpc.method1(sessionKey, values.getValue(EmvTags.APPLICATION_CRYPTOGRAM), "00".toByteArray())
        assertThat(response.arpc).isEqualTo(expected.toHex())
        assertThat(response.issuerAuthenticationData).isEqualTo(expected.toHex() + "3030")
    }

    @Test
    fun `rejects a changed amount`() = runTest {
        val field55 = replace(tap(amount = 100), EmvTags.AMOUNT_AUTHORISED, Bcd.encode(1, 6))

        val response = issuer.authorize(request(field55))

        assertThat(response.declineReason).isEqualTo(DeclineReason.CRYPTOGRAM_MISMATCH)
        assertThat(response.responseCode).isEqualTo(ResponseCode.SECURITY_VIOLATION)
        assertThat(response.arpc).isNull()
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE)
    }

    @Test
    fun `rejects a replayed cryptogram`() = runTest {
        val field55 = tap(amount = 100)

        assertThat(issuer.authorize(request(field55)).isApproved).isTrue()
        val replay = issuer.authorize(request(field55))

        assertThat(replay.declineReason).isEqualTo(DeclineReason.REPLAYED_COUNTER)
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - 100)
    }

    @Test
    fun `accepts taps that arrive after a skipped counter`() = runTest {
        tap(amount = 100)
        val second = tap(amount = 100)

        val response = issuer.authorize(request(second))

        assertThat(response.isApproved).isTrue()
        assertThat(response.atc).isEqualTo(2)
    }

    @Test
    fun `rejects a cryptogram made with another issuer's key`() = runTest {
        val other = IssuerHost("00112233445566778899AABBCCDDEEFF".hexToBytes(), listOf(DemoIssuer.account()))

        val response = other.authorize(request(tap(amount = 100)))

        assertThat(response.declineReason).isEqualTo(DeclineReason.CRYPTOGRAM_MISMATCH)
    }

    @Test
    fun `declines above the balance but still authenticates the answer`() = runTest {
        val response = issuer.authorize(request(tap(amount = DemoIssuer.OPENING_BALANCE + 1)))

        assertThat(response.declineReason).isEqualTo(DeclineReason.INSUFFICIENT_FUNDS)
        assertThat(response.responseCode.code).isEqualTo("51")
        assertThat(response.arpc).isNotNull()
        assertThat(issuer.account(DemoCards.lab.pan, 1)!!.lastAtc).isEqualTo(1)
    }

    @Test
    fun `declines an expired account`() = runTest {
        val field55 = tap(amount = 100)

        val response = issuer.authorize(AuthorizationRequest(DemoCards.lab.pan, 1, field55, LocalDate.of(2032, 1, 1)))

        assertThat(response.declineReason).isEqualTo(DeclineReason.EXPIRED_CARD)
    }

    @Test
    fun `refuses unknown cards and incomplete data`() = runTest {
        val field55 = tap(amount = 100)

        assertThat(issuer.authorize(AuthorizationRequest("4111111111111111", 0, field55, date)).declineReason)
            .isEqualTo(DeclineReason.UNKNOWN_ACCOUNT)
        val withoutUn = BerTlv.parse(field55).filterNot { it.tag == EmvTags.UNPREDICTABLE_NUMBER }.encode()
        assertThat(issuer.authorize(request(withoutUn)).declineReason).isEqualTo(DeclineReason.MISSING_DATA)
        assertThat(issuer.authorize(request("5A".hexToBytes())).declineReason).isEqualTo(DeclineReason.MISSING_DATA)
    }

    @Test
    fun `reset brings back the opening balance and forgets counters`() = runTest {
        val field55 = tap(amount = 100)
        issuer.authorize(request(field55))

        issuer.reset(listOf(DemoIssuer.account()))

        assertThat(issuer.account(DemoCards.lab.pan, 1)).isEqualTo(DemoIssuer.account())
        assertThat(issuer.authorize(request(field55)).isApproved).isTrue()
    }

    @Test
    fun `refuses an unknown cryptogram version`() = runTest {
        val field55 = replace(tap(amount = 100), EmvTags.ISSUER_APPLICATION_DATA, "06017703A00000".hexToBytes())

        assertThat(issuer.authorize(request(field55)).declineReason).isEqualTo(DeclineReason.UNKNOWN_CRYPTOGRAM_VERSION)
    }

    private suspend fun tap(amount: Long): ByteArray {
        val result = TerminalKernel().run(LoopbackTransport(card), TransactionRequest(amount = amount, date = date))
        return (result.outcome as TransactionOutcome.Completed).field55.hexToBytes()
    }

    private fun request(field55: ByteArray) = AuthorizationRequest(DemoCards.lab.pan, DemoCards.lab.panSequenceNumber, field55, date)

    private fun replace(field55: ByteArray, tag: Tag, value: ByteArray): ByteArray =
        BerTlv.parse(field55).map { if (it.tag == tag) Tlv.primitive(tag, value) else it }.encode()
}
