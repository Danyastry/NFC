package com.danyastry.nfcpay.core.emv.transport

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class ApduChannelTest {

    @Test
    fun `labels each exchange from the command unless told otherwise`() = runTest {
        val channel = ApduChannel(CardTransport { ResponseApdu.status(0x9000).encode() })

        channel.exchange(EmvCommands.select(EmvCommands.PPSE))
        channel.exchange(EmvCommands.readRecord(2, 1), label = "READ RECORD - the issuer certificate")

        assertThat(channel.exchanges.map { it.label }).containsExactly("SELECT PPSE", "READ RECORD - the issuer certificate").inOrder()
    }

    @Test
    fun `joins data a card hands out with 61XX`() = runTest {
        val parts = ArrayDeque(listOf(byteArrayOf(1, 2), byteArrayOf(3, 4), byteArrayOf(5)))
        val channel = ApduChannel(CardTransport { command ->
            val part = parts.removeFirst()
            val sw = if (parts.isEmpty()) 0x9000 else 0x6100 or parts.first().size
            if (CommandApdu.parse(command).ins == 0xC0 || part.contentEquals(byteArrayOf(1, 2))) ResponseApdu(part, sw).encode() else error("unexpected")
        })

        val response = channel.exchange(EmvCommands.select(EmvCommands.PPSE))

        assertThat(response.data.toHex()).isEqualTo("0102030405")
        assertThat(response.sw).isEqualTo(0x9000)
        assertThat(channel.exchanges.map { it.label }).containsExactly("SELECT PPSE", "GET RESPONSE", "GET RESPONSE").inOrder()
    }

    @Test
    fun `resends once with the length a card asks for with 6CXX`() = runTest {
        val sent = mutableListOf<CommandApdu>()
        val channel = ApduChannel(CardTransport { command ->
            val parsed = CommandApdu.parse(command).also(sent::add)
            if (parsed.le == 0x10) ResponseApdu(ByteArray(0x10), 0x9000).encode() else ResponseApdu.status(0x6C10).encode()
        })

        val response = channel.exchange(CommandApdu(0x00, 0xB0, 0x00, 0x00, le = 0x20))

        assertThat(response.data).hasLength(0x10)
        assertThat(sent.map { it.le }).containsExactly(0x20, 0x10).inOrder()
        assertThat(channel.exchanges.last().label).isEqualTo("READ BINARY - offset 0 - 32 bytes - retry with Le 10")
    }

    @Test
    fun `gives up on a card that never stops sending 61XX`() = runTest {
        val channel = ApduChannel(CardTransport { ResponseApdu(byteArrayOf(0), 0x6110).encode() })

        val failure = assertThrows(ResponseChainException::class.java) {
            kotlinx.coroutines.runBlocking { channel.exchange(EmvCommands.select(EmvCommands.PPSE)) }
        }

        assertThat(failure.sw).isEqualTo(0x6110)
        assertThat(channel.exchanges.count { it.label == "GET RESPONSE" }).isEqualTo(ApduChannel.MAX_GET_RESPONSE)
    }
}
