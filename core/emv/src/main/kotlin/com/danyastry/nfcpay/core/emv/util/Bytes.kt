package com.danyastry.nfcpay.core.emv.util

fun Int.toBytes(length: Int): ByteArray = ByteArray(length) { index ->
    (this ushr (8 * (length - 1 - index))).toByte()
}

fun ByteArray.toUnsignedInt(): Int = fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }

infix fun ByteArray.xor(other: ByteArray): ByteArray {
    require(size == other.size) { "XOR needs equal lengths, got $size and ${other.size}" }
    return ByteArray(size) { (this[it].toInt() xor other[it].toInt()).toByte() }
}
