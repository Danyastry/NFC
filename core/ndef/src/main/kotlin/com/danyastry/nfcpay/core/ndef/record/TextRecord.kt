package com.danyastry.nfcpay.core.ndef.record

import com.danyastry.nfcpay.core.ndef.message.NdefFormatException
import com.danyastry.nfcpay.core.ndef.message.NdefRecord
import com.danyastry.nfcpay.core.ndef.message.Tnf

data class TextRecord(val text: String, val language: String = "en") {

    init {
        require(language.isNotEmpty() && language.length <= MAX_LANGUAGE && language.all { it.code < 0x80 }) {
            "Language code must be 1 to $MAX_LANGUAGE ASCII characters"
        }
    }

    fun toRecord(): NdefRecord {
        val languageBytes = language.toByteArray(Charsets.US_ASCII)
        val payload = byteArrayOf(languageBytes.size.toByte()) + languageBytes + text.toByteArray(Charsets.UTF_8)
        return NdefRecord(Tnf.WELL_KNOWN, TYPE.toByteArray(Charsets.US_ASCII), payload)
    }

    companion object {
        const val TYPE = "T"
        private const val UTF16_FLAG = 0x80
        private const val LANGUAGE_MASK = 0x3F
        private const val MAX_LANGUAGE = 63

        fun from(record: NdefRecord): TextRecord? {
            if (!record.isType(Tnf.WELL_KNOWN, TYPE)) return null
            val payload = record.payload
            if (payload.isEmpty()) throw NdefFormatException("Text record has no status byte")
            val status = payload[0].toInt() and 0xFF
            val languageLength = status and LANGUAGE_MASK
            if (1 + languageLength > payload.size) throw NdefFormatException("Language code runs past the payload")
            val language = payload.copyOfRange(1, 1 + languageLength).toString(Charsets.US_ASCII)
            val charset = if (status and UTF16_FLAG != 0) Charsets.UTF_16 else Charsets.UTF_8
            val text = payload.copyOfRange(1 + languageLength, payload.size).toString(charset)
            return TextRecord(text, language.ifEmpty { "en" })
        }
    }
}
