package com.danyastry.nfcpay.core.emv.util

object Bcd {

    fun encode(value: Long, length: Int): ByteArray {
        require(value >= 0) { "BCD holds only non-negative numbers, got $value" }
        return encodeDigits(value.toString(), length)
    }

    fun encodeDigits(digits: String, length: Int): ByteArray {
        require(digits.all { it in '0'..'9' }) { "BCD holds only digits, got '$digits'" }
        require(digits.length <= length * 2) { "'$digits' does not fit into $length BCD bytes" }
        return digits.padStart(length * 2, '0').hexToBytes()
    }

    fun decode(bytes: ByteArray): String = bytes.toHex()

    fun decodeNumber(bytes: ByteArray): Long = decode(bytes).toLong()
}
