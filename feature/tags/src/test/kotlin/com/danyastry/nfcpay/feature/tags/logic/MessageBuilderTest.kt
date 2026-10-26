package com.danyastry.nfcpay.feature.tags.logic

import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.message.NdefRecord
import com.danyastry.nfcpay.core.ndef.message.Tnf
import com.danyastry.nfcpay.core.ndef.record.UriRecord
import com.danyastry.nfcpay.feature.tags.model.RecordKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageBuilderTest {

    @Test
    fun `adds https to bare links and keeps other schemes`() {
        assertThat(UriRecord.from(MessageBuilder.build(RecordKind.LINK, "example.com")!!.records.single())!!.uri).isEqualTo("https://example.com")
        assertThat(UriRecord.from(MessageBuilder.build(RecordKind.LINK, "tel:+380441234567")!!.records.single())!!.uri).isEqualTo("tel:+380441234567")
    }

    @Test
    fun `blank input builds nothing`() {
        assertThat(MessageBuilder.build(RecordKind.TEXT, "   ")).isNull()
    }

    @Test
    fun `describes text, link and other records`() {
        val message = NdefMessage(
            MessageBuilder.build(RecordKind.TEXT, "hi")!!.records.single(),
            MessageBuilder.build(RecordKind.LINK, "https://www.example.com")!!.records.single(),
            NdefRecord(Tnf.MEDIA, "text/plain".toByteArray(), "x".toByteArray()),
        )

        val views = RecordViews.of(message)

        assertThat(views.map { it.kind }).containsExactly("Text", "Link", "Media type").inOrder()
        assertThat(views[1].detail).startsWith("prefix code 02")
        assertThat(views[2].content).isEqualTo("text/plain")
    }
}
