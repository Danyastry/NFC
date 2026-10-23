package com.danyastry.nfcpay.core.emv.util

object Hex {

    private const val DIGITS = "0123456789ABCDEF"

    fun encode(bytes: ByteArray): String = buildString(bytes.size * 2) {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xFF
            append(DIGITS[value ushr 4])
            append(DIGITS[value and 0x0F])
        }
    }

    fun decode(text: String): ByteArray {
        val clean = text.filterNot { it.isWhitespace() }
        require(clean.length % 2 == 0) { "Hex needs an even number of digits, got ${clean.length}" }
        return ByteArray(clean.length / 2) { index ->
            val high = Character.digit(clean[index * 2], 16)
            val low = Character.digit(clean[index * 2 + 1], 16)
            require(high >= 0 && low >= 0) { "Not a hex digit near position ${index * 2}" }
            ((high shl 4) or low).toByte()
        }
    }

    fun isValid(text: String): Boolean = runCatching { decode(text) }.isSuccess
}

fun ByteArray.toHex(): String = Hex.encode(this)

fun String.hexToBytes(): ByteArray = Hex.decode(this)
