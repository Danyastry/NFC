package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class TransportQuirksTest {

    private val request = TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 26))

    @Test
    fun `fetches data a card holds back with 61XX`() = runTest {
        val card = ChunkingCard(VirtualCard(DemoCards.lab), chunk = 20)

        val result = TerminalKernel().run(card, request)

        val outcome = result.outcome as TransactionOutcome.Completed
        assertThat(outcome.decision).isEqualTo(CryptogramType.ARQC)
        assertThat(outcome.card.pan).isEqualTo(DemoCards.lab.pan)
        assertThat(result.exchanges.count { it.label == "GET RESPONSE" }).isAtLeast(6)
        assertThat(result.exchanges.filter { it.label == "GET RESPONSE" }.all { it.command.ins == 0xC0 }).isTrue()
    }

    @Test
    fun `resends with the length a card asks for with 6CXX`() = runTest {
        val card = StrictLengthCard(VirtualCard(DemoCards.lab))

        val result = TerminalKernel().run(card, request)

        assertThat(result.outcome).isInstanceOf(TransactionOutcome.Completed::class.java)
        assertThat(result.exchanges.map { it.label }).contains("SELECT PPSE")
        assertThat(result.exchanges.any { it.label.startsWith("SELECT PPSE - retry with Le") }).isTrue()
    }

    @Test
    fun `gives up on a card that never stops sending 61XX`() = runTest {
        val endless = CardTransport { command ->
            if (CommandApdu.parse(command).ins == 0xC0) byteArrayOf(0x00, 0x61, 0x10) else byteArrayOf(0x61, 0x10)
        }

        val result = TerminalKernel().run(endless, request)

        assertThat(result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.MALFORMED_RESPONSE, 0x6110))
        assertThat(result.exchanges.count { it.label == "GET RESPONSE" }).isEqualTo(TerminalKernel.MAX_GET_RESPONSE)
    }

    private class ChunkingCard(private val card: VirtualCard, private val chunk: Int) : CardTransport {
        private var pending = ByteArray(0)
        private var pendingSw = StatusWord.SUCCESS

        override suspend fun transceive(command: ByteArray): ByteArray {
            val apdu = CommandApdu.parse(command)
            if (apdu.ins == 0xC0) {
                val size = minOf(apdu.le ?: 0, pending.size)
                val part = pending.copyOfRange(0, size)
                pending = pending.copyOfRange(size, pending.size)
                return if (pending.isEmpty()) ResponseApdu(part, pendingSw).encode() else ResponseApdu(part, 0x6100 or minOf(pending.size, chunk)).encode()
            }
            val response = ResponseApdu.parse(card.process(command))
            if (response.data.isEmpty()) return response.encode()
            pending = response.data
            pendingSw = response.sw
            return ResponseApdu.status(0x6100 or minOf(pending.size, chunk)).encode()
        }
    }

    private class StrictLengthCard(private val card: VirtualCard) : CardTransport {
        override suspend fun transceive(command: ByteArray): ByteArray {
            val apdu = CommandApdu.parse(command)
            val response = ResponseApdu.parse(card.process(command))
            if (apdu.ins == 0xA4 && response.isSuccess && apdu.le != response.data.size) {
                card.deactivate()
                return ResponseApdu.status(0x6C00 or response.data.size).encode()
            }
            return response.encode()
        }
    }
}
