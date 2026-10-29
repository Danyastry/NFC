package com.danyastry.nfcpay.feature.tags.logic

import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.ndef.message.NdefFormatException
import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.record.UriRecord
import com.danyastry.nfcpay.feature.tags.model.RecordView

object RecordViews {

    fun of(message: NdefMessage): List<RecordView> = message.records.map { record ->
        val text = runCatching { TextRecord.from(record) }.getOrNull()
        val uri = runCatching { UriRecord.from(record) }.getOrNull()
        when {
            text != null -> RecordView("Text", text.text, "language ${text.language} - ${record.payload.size} bytes")
            uri != null -> RecordView("Link", uri.uri, "prefix code %02X - ${record.payload.size} bytes".format(record.payload[0].toInt() and 0xFF))
            else -> RecordView(record.tnf.label, record.typeName.ifEmpty { "(no type)" }, "${record.payload.size} bytes - ${record.payload.toHex().take(32)}")
        }
    }

    fun parse(bytes: ByteArray): Result<NdefMessage> = try {
        Result.success(NdefMessage.parse(bytes))
    } catch (error: NdefFormatException) {
        Result.failure(error)
    }
}
