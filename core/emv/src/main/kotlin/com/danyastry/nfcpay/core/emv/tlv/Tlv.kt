package com.danyastry.nfcpay.core.emv.tlv

import com.danyastry.nfcpay.core.emv.util.toHex

class Tlv private constructor(
    val tag: Tag,
    val value: ByteArray,
    val children: List<Tlv>,
) {
    val isConstructed: Boolean
        get() = tag.isConstructed

    fun encode(): ByteArray = tag.bytes + BerTlv.encodeLength(value.size) + value

    fun find(target: Tag): Tlv? = if (tag == target) this else children.findTag(target)

    override fun equals(other: Any?): Boolean =
        other is Tlv && tag == other.tag && value.contentEquals(other.value) && children == other.children

    override fun hashCode(): Int = 31 * tag.hashCode() + value.contentHashCode()

    override fun toString(): String = if (children.isEmpty()) "${tag.hex}=${value.toHex()}" else "${tag.hex}$children"

    companion object {
        fun primitive(tag: Tag, value: ByteArray): Tlv = Tlv(tag, value, emptyList())

        fun constructed(tag: Tag, vararg children: Tlv): Tlv = constructed(tag, children.toList())

        fun constructed(tag: Tag, children: List<Tlv>): Tlv {
            require(tag.isConstructed) { "Tag ${tag.hex} is primitive, it cannot hold children" }
            return Tlv(tag, children.fold(ByteArray(0)) { acc, child -> acc + child.encode() }, children)
        }

        internal fun parsed(tag: Tag, value: ByteArray, children: List<Tlv>): Tlv = Tlv(tag, value, children)
    }
}

fun List<Tlv>.findTag(tag: Tag): Tlv? = firstNotNullOfOrNull { it.find(tag) }

fun List<Tlv>.valueOf(tag: Tag): ByteArray? = findTag(tag)?.value

fun List<Tlv>.flatten(): List<Tlv> = flatMap { listOf(it) + it.children.flatten() }

fun List<Tlv>.encode(): ByteArray = fold(ByteArray(0)) { acc, tlv -> acc + tlv.encode() }
