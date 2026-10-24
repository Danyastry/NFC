package com.danyastry.nfcpay.core.emv.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object TripleDes {

    const val BLOCK_SIZE = 8
    const val DOUBLE_KEY_SIZE = 16

    fun encrypt(key: ByteArray, block: ByteArray): ByteArray = run(Cipher.ENCRYPT_MODE, key, block)

    fun decrypt(key: ByteArray, block: ByteArray): ByteArray = run(Cipher.DECRYPT_MODE, key, block)

    @Suppress("GetInstance")
    private fun run(mode: Int, key: ByteArray, block: ByteArray): ByteArray {
        require(key.size == DOUBLE_KEY_SIZE) { "Expected a double-length key, got ${key.size} bytes" }
        require(block.size == BLOCK_SIZE) { "3DES works on $BLOCK_SIZE-byte blocks, got ${block.size}" }
        return Cipher.getInstance("DESede/ECB/NoPadding").run {
            init(mode, SecretKeySpec(key + key.copyOfRange(0, BLOCK_SIZE), "DESede"))
            doFinal(block)
        }
    }
}
