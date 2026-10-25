package com.danyastry.nfcpay.core.ndef.message

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.record.UriRecord
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class NdefMessageTest {

    @Test
    fun `encodes a short well-known text record`() {
        val message = NdefMessage(TextRecord("Hello, world!").toRecord())

        assertThat(message.encode().toHex()).isEqualTo("D101105402656E48656C6C6F2C20776F726C6421")
    }

    @Test
    fun `encodes a URI with the https www prefix`() {
        val message = NdefMessage(UriRecord("https://www.example.com").toRecord())

        assertThat(message.encode().toHex()).isEqualTo("D1010C5502" + "6578616D706C652E636F6D")
    }

    @Test
    fun `sets begin and end only on the first and last records`() {
        val bytes = NdefMessage(TextRecord("a").toRecord(), TextRecord("b").toRecord(), UriRecord("tel:123").toRecord()).encode()

        val parsed = NdefMessage.parse(bytes)
        assertThat(parsed.records).hasSize(3)
        assertThat(bytes[0].toInt() and 0xC0).isEqualTo(0x80)
        assertThat(UriRecord.from(parsed.records[2])).isEqualTo(UriRecord("tel:123"))
    }

    @Test
    fun `uses a four byte length for long payloads`() {
        val long = TextRecord("x".repeat(300)).toRecord()

        val bytes = NdefMessage(long).encode()

        assertThat(bytes[0].toInt() and NdefMessage.SHORT_RECORD).isEqualTo(0)
        assertThat(bytes.copyOfRange(2, 6).toHex()).isEqualTo("0000012F")
        assertThat(NdefMessage.parse(bytes).records.single()).isEqualTo(long)
    }

    @Test
    fun `round trips ids and media records`() {
        val record = NdefRecord(Tnf.MEDIA, "text/plain".toByteArray(), "hi".toByteArray(), id = "1".toByteArray())

        assertThat(NdefMessage.parse(NdefMessage(record).encode()).records.single()).isEqualTo(record)
    }

    @Test
    fun `rejects malformed messages`() {
        listOf(
            "",
            "5101015400",
            "D1010554",
            "B101015400",
            "D101015400FF",
            "9101015400D1010154",
            "D601015400",
        ).forEach { hex ->
            assertThrows(NdefFormatException::class.java) { NdefMessage.parse(hex.hexToBytes()) }
        }
    }
}
