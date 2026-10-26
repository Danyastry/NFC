package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.record.UriRecord
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class Type4ReaderTest {

    private val reader = Type4Reader()
    private val hello = NdefMessage(TextRecord("Hello, tag").toRecord(), UriRecord("https://www.example.com").toRecord())

    @Test
    fun `reads a message with the NFC Forum procedure`() = runTest {
        val tag = Type4Tag(initial = hello)

        val session = reader.read(LoopbackTransport(tag))

        assertThat(session.error).isNull()
        assertThat(session.message).isEqualTo(hello)
        assertThat(session.exchanges.map { it.label }).containsExactly(
            "SELECT NDEF application",
            "SELECT capability container",
            "READ BINARY · capability container",
            "SELECT NDEF file E104",
            "READ BINARY · offset 0 · 2 bytes",
            "READ BINARY · offset 2 · ${hello.encode().size} bytes",
        ).inOrder()
    }

    @Test
    fun `writes in MLc chunks and sets NLEN last`() = runTest {
        val tag = Type4Tag(CapabilityContainer(maxWriteSize = 16))
        val long = NdefMessage(TextRecord("x".repeat(60)).toRecord())

        val session = reader.write(LoopbackTransport(tag), long)

        assertThat(session.isSuccess).isTrue()
        val labels = session.exchanges.map { it.label }
        assertThat(labels[4]).isEqualTo("UPDATE BINARY · NLEN 0000")
        assertThat(labels.last()).startsWith("UPDATE BINARY · NLEN 00")
        assertThat(labels.count { it.contains("offset") }).isEqualTo((long.encode().size + 15) / 16)
        assertThat(tag.message).isEqualTo(long)
    }

    @Test
    fun `reads long messages in MLe chunks`() = runTest {
        val long = NdefMessage(TextRecord("y".repeat(200)).toRecord())
        val tag = Type4Tag(CapabilityContainer(maxReadSize = 0x20), long)

        val session = reader.read(LoopbackTransport(tag))

        assertThat(session.message).isEqualTo(long)
        assertThat(session.exchanges.count { it.label.startsWith("READ BINARY · offset") && it.label != "READ BINARY · offset 0 · 2 bytes" })
            .isEqualTo((long.encode().size + 0x1F) / 0x20)
    }

    @Test
    fun `what one reader writes another reads`() = runTest {
        val tag = Type4Tag()

        reader.write(LoopbackTransport(tag), hello)

        assertThat(Type4Reader().read(LoopbackTransport(tag)).message).isEqualTo(hello)
    }

    @Test
    fun `reports an empty tag, a read-only tag and a message that does not fit`() = runTest {
        assertThat(reader.read(LoopbackTransport(Type4Tag())).error).isEqualTo(TagError.EMPTY)
        val locked = Type4Tag(CapabilityContainer(writeAccess = CapabilityContainer.ACCESS_DENIED), hello)
        assertThat(reader.write(LoopbackTransport(locked), hello).error).isEqualTo(TagError.READ_ONLY)
        val small = Type4Tag(CapabilityContainer(maxNdefFileSize = 20))
        assertThat(reader.write(LoopbackTransport(small), hello).error).isEqualTo(TagError.TOO_LARGE)
    }

    @Test
    fun `a payment card is not an NDEF tag`() = runTest {
        val session = reader.read { ResponseApdu.status(StatusWord.FILE_NOT_FOUND).encode() }

        assertThat(session.error).isEqualTo(TagError.NOT_AN_NDEF_TAG)
        assertThat(session.exchanges).hasSize(1)
    }

    @Test
    fun `a tag that leaves mid-read is lost`() = runTest {
        val tag = Type4Tag(initial = hello)
        var count = 0
        val leaving = CardTransport { if (++count > 4) throw IOException("gone") else tag.process(it) }

        assertThat(reader.read(leaving).error).isEqualTo(TagError.TAG_LOST)
    }

    @Test
    fun `garbage in the NDEF file is reported`() = runTest {
        val tag = Type4Tag()
        val transport = LoopbackTransport(tag)
        reader.write(transport, hello)
        tag.process(CommandApdu(0x00, 0xA4, 0x04, 0x00, Type4Files.NDEF_APPLICATION).encode())
        tag.process(CommandApdu(0x00, 0xA4, 0x00, 0x0C, byteArrayOf(0xE1.toByte(), 0x04)).encode())
        tag.process(CommandApdu(0x00, 0xD6, 0x00, 0x02, byteArrayOf(0x00, 0x00, 0x00)).encode())

        assertThat(reader.read(transport).error).isEqualTo(TagError.MALFORMED_MESSAGE)
    }
}
