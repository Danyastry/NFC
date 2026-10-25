package com.danyastry.nfcpay.core.emv.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object Aes {

    const val BLOCK_SIZE = 16

    fun encrypt(key: ByteArray, block: ByteArray): ByteArray = run(Cipher.ENCRYPT_MODE, key, block)

    fun decrypt(key: ByteArray, block: ByteArray): ByteArray = run(Cipher.DECRYPT_MODE, key, block)

    @Suppress("GetInstance")
    private fun run(mode: Int, key: ByteArray, block: ByteArray): ByteArray {
        require(key.size in setOf(16, 24, 32)) { "AES keys are 16, 24 or 32 bytes, got ${key.size}" }
        require(block.size == BLOCK_SIZE) { "AES works on $BLOCK_SIZE-byte blocks, got ${block.size}" }
        return Cipher.getInstance("AES/ECB/NoPadding").run {
            init(mode, SecretKeySpec(key, "AES"))
            doFinal(block)
        }
    }
}
