package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.toBytes

object LimitedUseKeyDerivation {

    const val MAX_INDEX = 0xFF
    private const val LEFT_MARKER = 0xA5
    private const val RIGHT_MARKER = 0x5A

    fun derive(tokenMasterKey: ByteArray, index: Int): ByteArray {
        require(index in 1..MAX_INDEX) { "Key index must be 1..$MAX_INDEX, got $index" }
        return DesParity.adjust(
            TripleDes.encrypt(tokenMasterKey, diversification(index, LEFT_MARKER)) +
                TripleDes.encrypt(tokenMasterKey, diversification(index, RIGHT_MARKER)),
        )
    }

    internal fun diversification(index: Int, marker: Int): ByteArray =
        index.toBytes(2) + byteArrayOf(marker.toByte()) + ByteArray(TripleDes.BLOCK_SIZE - 3)
}
