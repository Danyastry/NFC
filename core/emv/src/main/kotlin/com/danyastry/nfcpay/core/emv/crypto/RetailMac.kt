package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.xor
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object RetailMac {

    const val BLOCK_SIZE = 8
    const val KEY_SIZE = 16

    fun compute(key: ByteArray, data: ByteArray): ByteArray = computeAlgorithm3(key, padMethod2(data))

    fun computeZeroPadded(key: ByteArray, data: ByteArray): ByteArray = computeAlgorithm3(key, padMethod1(data))

    fun padMethod1(data: ByteArray): ByteArray {
        val remainder = data.size % BLOCK_SIZE
        return if (data.isNotEmpty() && remainder == 0) data else data + ByteArray(BLOCK_SIZE - remainder)
    }

    fun padMethod2(data: ByteArray): ByteArray {
        val padded = data + byteArrayOf(0x80.toByte())
        val remainder = padded.size % BLOCK_SIZE
        return if (remainder == 0) padded else padded + ByteArray(BLOCK_SIZE - remainder)
    }

    fun computeAlgorithm3(key: ByteArray, padded: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) { "Retail MAC uses a double-length DES key, got ${key.size} bytes" }
        require(padded.isNotEmpty() && padded.size % BLOCK_SIZE == 0) { "Data must be padded to whole blocks" }
        val left = key.copyOfRange(0, BLOCK_SIZE)
        val right = key.copyOfRange(BLOCK_SIZE, KEY_SIZE)
        var chain = ByteArray(BLOCK_SIZE)
        padded.toList().chunked(BLOCK_SIZE).forEach { block ->
            chain = des(Cipher.ENCRYPT_MODE, left, chain xor block.toByteArray())
        }
        return des(Cipher.ENCRYPT_MODE, left, des(Cipher.DECRYPT_MODE, right, chain))
    }

    @Suppress("GetInstance")
    private fun des(mode: Int, key: ByteArray, block: ByteArray): ByteArray =
        Cipher.getInstance("DESede/ECB/NoPadding").run {
            init(mode, SecretKeySpec(key + key + key, "DESede"))
            doFinal(block)
        }
}
