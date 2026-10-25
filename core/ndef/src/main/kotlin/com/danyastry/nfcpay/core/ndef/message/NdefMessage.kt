package com.danyastry.nfcpay.core.ndef.message

import java.io.ByteArrayOutputStream

class NdefMessage(val records: List<NdefRecord>) {

    init {
        require(records.isNotEmpty()) { "An NDEF message has at least one record" }
    }

    constructor(vararg records: NdefRecord) : this(records.toList())

    fun encode(): ByteArray {
        val out = ByteArrayOutputStream()
        records.forEachIndexed { index, record ->
            val short = record.payload.size <= 0xFF
            var header = record.tnf.code
            if (index == 0) header = header or MESSAGE_BEGIN
            if (index == records.lastIndex) header = header or MESSAGE_END
            if (short) header = header or SHORT_RECORD
            if (record.id.isNotEmpty()) header = header or ID_LENGTH_PRESENT
            out.write(header)
            out.write(record.type.size)
            if (short) {
                out.write(record.payload.size)
            } else {
                val length = record.payload.size
                listOf(24, 16, 8, 0).forEach { shift -> out.write(length ushr shift and 0xFF) }
            }
            if (record.id.isNotEmpty()) out.write(record.id.size)
            out.write(record.type)
            out.write(record.id)
            out.write(record.payload)
        }
        return out.toByteArray()
    }

    override fun equals(other: Any?): Boolean = other is NdefMessage && records == other.records

    override fun hashCode(): Int = records.hashCode()

    companion object {
        const val MESSAGE_BEGIN = 0x80
        const val MESSAGE_END = 0x40
        const val CHUNK = 0x20
        const val SHORT_RECORD = 0x10
        const val ID_LENGTH_PRESENT = 0x08

        fun parse(bytes: ByteArray): NdefMessage {
            val records = mutableListOf<NdefRecord>()
            var position = 0
            fun next(what: String): Int {
                if (position >= bytes.size) throw NdefFormatException("Message ends inside the $what")
                return bytes[position++].toInt() and 0xFF
            }
            fun take(count: Int, what: String): ByteArray {
                if (count < 0 || position + count > bytes.size) throw NdefFormatException("Message ends inside the $what")
                return bytes.copyOfRange(position, position + count).also { position += count }
            }
            while (true) {
                val header = next("record header")
                if (records.isEmpty() && header and MESSAGE_BEGIN == 0) throw NdefFormatException("First record must have MB set")
                if (records.isNotEmpty() && header and MESSAGE_BEGIN != 0) throw NdefFormatException("Only the first record may have MB set")
                if (header and CHUNK != 0) throw NdefFormatException("Chunked records are not supported")
                val typeLength = next("type length")
                val payloadLength = if (header and SHORT_RECORD != 0) {
                    next("payload length")
                } else {
                    (0 until 4).fold(0L) { acc, _ -> (acc shl 8) or next("payload length").toLong() }
                        .also { if (it > Int.MAX_VALUE) throw NdefFormatException("Payload too large") }
                        .toInt()
                }
                val idLength = if (header and ID_LENGTH_PRESENT != 0) next("id length") else 0
                val type = take(typeLength, "type")
                val id = take(idLength, "id")
                val payload = take(payloadLength, "payload")
                val tnf = Tnf.of(header)
                if (tnf == Tnf.UNCHANGED || tnf == Tnf.RESERVED) throw NdefFormatException("TNF ${tnf.name} is not allowed here")
                records += try {
                    NdefRecord(tnf, type, payload, id)
                } catch (error: IllegalArgumentException) {
                    throw NdefFormatException(error.message.orEmpty())
                }
                if (header and MESSAGE_END != 0) break
            }
            if (position != bytes.size) throw NdefFormatException("${bytes.size - position} bytes follow the last record")
            return NdefMessage(records)
        }
    }
}
