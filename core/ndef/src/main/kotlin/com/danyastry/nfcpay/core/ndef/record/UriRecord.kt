package com.danyastry.nfcpay.core.ndef.record

import com.danyastry.nfcpay.core.ndef.message.NdefFormatException
import com.danyastry.nfcpay.core.ndef.message.NdefRecord
import com.danyastry.nfcpay.core.ndef.message.Tnf

data class UriRecord(val uri: String) {

    fun toRecord(): NdefRecord {
        val (code, rest) = UriPrefixes.abbreviate(uri)
        return NdefRecord(Tnf.WELL_KNOWN, TYPE.toByteArray(Charsets.US_ASCII), byteArrayOf(code.toByte()) + rest.toByteArray(Charsets.UTF_8))
    }

    companion object {
        const val TYPE = "U"

        fun from(record: NdefRecord): UriRecord? {
            if (!record.isType(Tnf.WELL_KNOWN, TYPE)) return null
            if (record.payload.isEmpty()) throw NdefFormatException("URI record has no prefix byte")
            val code = record.payload[0].toInt() and 0xFF
            val rest = record.payload.copyOfRange(1, record.payload.size).toString(Charsets.UTF_8)
            return UriRecord(UriPrefixes.expand(code, rest))
        }
    }
}
