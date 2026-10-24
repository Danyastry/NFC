package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.xor

object Arpc {

    const val RESPONSE_CODE_LENGTH = 2

    fun method1(sessionKey: ByteArray, arqc: ByteArray, authorisationResponseCode: ByteArray): ByteArray {
        require(arqc.size == TripleDes.BLOCK_SIZE) { "ARQC is 8 bytes, got ${arqc.size}" }
        require(authorisationResponseCode.size == RESPONSE_CODE_LENGTH) { "ARC is 2 bytes, got ${authorisationResponseCode.size}" }
        val padded = authorisationResponseCode + ByteArray(TripleDes.BLOCK_SIZE - RESPONSE_CODE_LENGTH)
        return TripleDes.encrypt(sessionKey, arqc xor padded)
    }
}
