package com.danyastry.nfcpay.core.emv.dol

import com.danyastry.nfcpay.core.emv.tags.EmvDictionary
import com.danyastry.nfcpay.core.emv.tags.TagFormat
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException

object Dol {

    fun parse(bytes: ByteArray): List<DolEntry> {
        val entries = mutableListOf<DolEntry>()
        var position = 0
        while (position < bytes.size) {
            val (tag, afterTag) = BerTlv.readTag(bytes, position, bytes.size)
            if (afterTag >= bytes.size) throw TlvFormatException("DOL entry ${tag.hex} has no length", position)
            entries += DolEntry(tag, bytes[afterTag].toInt() and 0xFF)
            position = afterTag + 1
        }
        return entries
    }

    fun encode(entries: List<DolEntry>): ByteArray =
        entries.fold(ByteArray(0)) { acc, entry -> acc + entry.tag.bytes + byteArrayOf(entry.length.toByte()) }

    fun totalLength(entries: List<DolEntry>): Int = entries.sumOf { it.length }

    fun build(entries: List<DolEntry>, valueOf: (Tag) -> ByteArray?): ByteArray =
        entries.fold(ByteArray(0)) { acc, entry -> acc + fit(entry, valueOf(entry.tag)) }

    fun split(entries: List<DolEntry>, data: ByteArray): Map<Tag, ByteArray> {
        require(data.size == totalLength(entries)) { "DOL needs ${totalLength(entries)} bytes, got ${data.size}" }
        var position = 0
        return entries.associate { entry ->
            entry.tag to data.copyOfRange(position, position + entry.length).also { position += entry.length }
        }
    }

    internal fun fit(entry: DolEntry, value: ByteArray?): ByteArray {
        if (value == null) return ByteArray(entry.length)
        val numeric = EmvDictionary.info(entry.tag)?.format == TagFormat.NUMERIC
        return when {
            value.size == entry.length -> value
            value.size > entry.length && numeric -> value.copyOfRange(value.size - entry.length, value.size)
            value.size > entry.length -> value.copyOfRange(0, entry.length)
            numeric -> ByteArray(entry.length - value.size) + value
            else -> value + ByteArray(entry.length - value.size)
        }
    }
}
