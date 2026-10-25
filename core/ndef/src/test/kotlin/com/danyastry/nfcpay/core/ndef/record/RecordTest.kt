package com.danyastry.nfcpay.core.ndef.record

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.ndef.message.NdefFormatException
import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.message.NdefRecord
import com.danyastry.nfcpay.core.ndef.message.Tnf
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordTest {

    @Test
    fun `reads text in any language and in UTF-16`() {
        val utf8 = TextRecord("Привіт", "uk")
        assertThat(TextRecord.from(utf8.toRecord())).isEqualTo(utf8)

        val utf16 = NdefRecord(Tnf.WELL_KNOWN, "T".toByteArray(), byteArrayOf(0x82.toByte()) + "en".toByteArray() + "Hi".toByteArray(Charsets.UTF_16))
        assertThat(TextRecord.from(utf16)!!.text).isEqualTo("Hi")
    }

    @Test
    fun `picks the longest URI prefix`() {
        assertThat(UriPrefixes.abbreviate("https://www.example.com")).isEqualTo(2 to "example.com")
        assertThat(UriPrefixes.abbreviate("https://example.com")).isEqualTo(4 to "example.com")
        assertThat(UriPrefixes.abbreviate("geo:50.45,30.52")).isEqualTo(0 to "geo:50.45,30.52")
        assertThat(UriPrefixes.ALL).hasSize(0x24)
    }

    @Test
    fun `expands prefixes when reading`() {
        val record = NdefMessage.parse("D1010C55026578616D706C652E636F6D".hexToBytes()).records.single()

        assertThat(UriRecord.from(record)).isEqualTo(UriRecord("https://www.example.com"))
        assertThat(TextRecord.from(record)).isNull()
    }

    @Test
    fun `rejects broken text and URI payloads`() {
        assertThrows(NdefFormatException::class.java) { TextRecord.from(NdefRecord(Tnf.WELL_KNOWN, "T".toByteArray())) }
        assertThrows(NdefFormatException::class.java) { TextRecord.from(NdefRecord(Tnf.WELL_KNOWN, "T".toByteArray(), byteArrayOf(0x05, 0x65))) }
        assertThrows(NdefFormatException::class.java) { UriRecord.from(NdefRecord(Tnf.WELL_KNOWN, "U".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { TextRecord("x", "") }
    }
}
