package com.danyastry.nfcpay.feature.tags.logic

import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.record.UriRecord
import com.danyastry.nfcpay.feature.tags.model.RecordKind

object MessageBuilder {

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

    fun build(kind: RecordKind, input: String): NdefMessage? {
        val value = input.trim()
        if (value.isEmpty()) return null
        val record = when (kind) {
            RecordKind.TEXT -> TextRecord(value).toRecord()
            RecordKind.LINK -> UriRecord(if (SCHEME.containsMatchIn(value)) value else "https://$value").toRecord()
        }
        return NdefMessage(record)
    }
}
