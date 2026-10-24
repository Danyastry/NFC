package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.toHex

object KeyCheckValue {

    const val LENGTH = 3

    fun of(key: ByteArray): String = TripleDes.encrypt(key, ByteArray(TripleDes.BLOCK_SIZE)).copyOfRange(0, LENGTH).toHex()
}
