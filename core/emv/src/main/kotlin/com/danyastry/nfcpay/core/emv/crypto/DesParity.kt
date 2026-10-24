package com.danyastry.nfcpay.core.emv.crypto

object DesParity {

    fun adjust(key: ByteArray): ByteArray = ByteArray(key.size) { index ->
        val high = key[index].toInt() and 0xFE
        (if (Integer.bitCount(high) % 2 == 0) high or 0x01 else high).toByte()
    }

    fun isOdd(key: ByteArray): Boolean = key.all { Integer.bitCount(it.toInt() and 0xFF) % 2 == 1 }
}
