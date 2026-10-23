package com.danyastry.nfcpay.core.emv.tlv

object BerTlv {

    private const val PADDING_ZERO = 0x00
    private const val PADDING_FF = 0xFF

    fun parse(bytes: ByteArray): List<Tlv> = Reader(bytes, 0, bytes.size).readAll()

    fun encodeLength(length: Int): ByteArray = when {
        length < 0x80 -> byteArrayOf(length.toByte())
        length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
        length <= 0xFFFF -> byteArrayOf(0x82.toByte(), (length ushr 8).toByte(), length.toByte())
        else -> throw IllegalArgumentException("Values longer than 65535 bytes are not used in EMV, got $length")
    }

    internal fun readTag(bytes: ByteArray, start: Int, end: Int): Pair<Tag, Int> {
        var position = start
        var value = bytes[position++].toInt() and 0xFF
        if (value == 0) throw TlvFormatException("Tag 00 is not valid", start)
        if (value and Tag.MORE_BYTES_MASK == Tag.MORE_BYTES_MASK) {
            do {
                if (position >= end) throw TlvFormatException("Tag runs past the end", start)
                val next = bytes[position++].toInt() and 0xFF
                value = (value shl 8) or next
                if (position - start > 3) throw TlvFormatException("Tag longer than three bytes", start)
            } while (next and Tag.NEXT_BYTE_BIT != 0)
        }
        return Tag(value) to position
    }

    private class Reader(private val bytes: ByteArray, private var position: Int, private val end: Int) {

        fun readAll(): List<Tlv> {
            val result = mutableListOf<Tlv>()
            while (position < end) {
                val first = bytes[position].toInt() and 0xFF
                if (first == PADDING_ZERO || first == PADDING_FF) {
                    position++
                    continue
                }
                result += readOne()
            }
            return result
        }

        private fun readOne(): Tlv {
            val start = position
            val (tag, afterTag) = readTag(bytes, position, end)
            position = afterTag
            val length = readLength(start)
            if (position + length > end) {
                throw TlvFormatException("Tag ${tag.hex} says $length bytes but only ${end - position} are left", start)
            }
            val value = bytes.copyOfRange(position, position + length)
            val children = if (tag.isConstructed) parseChildren(value) else emptyList()
            position += length
            return Tlv.parsed(tag, value, children)
        }

        private fun parseChildren(value: ByteArray): List<Tlv> =
            runCatching { Reader(value, 0, value.size).readAll() }.getOrDefault(emptyList())

        private fun readLength(tagStart: Int): Int {
            if (position >= end) throw TlvFormatException("Length is missing", tagStart)
            val first = bytes[position++].toInt() and 0xFF
            if (first < 0x80) return first
            val count = first and 0x7F
            if (count == 0 || count > 3) throw TlvFormatException("Unsupported length form 0x%02X".format(first), tagStart)
            if (position + count > end) throw TlvFormatException("Length runs past the end", tagStart)
            var length = 0
            repeat(count) { length = (length shl 8) or (bytes[position++].toInt() and 0xFF) }
            return length
        }
    }
}
