package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.toBytes

object SessionKeyDerivation {

    private const val LEFT_MARKER = 0xF0
    private const val RIGHT_MARKER = 0x0F

    fun commonSessionKey(cardMasterKey: ByteArray, atc: Int): ByteArray {
        require(atc in 0..0xFFFF) { "ATC is two bytes, got $atc" }
        return DesParity.adjust(
            TripleDes.encrypt(cardMasterKey, diversification(atc, LEFT_MARKER)) +
                TripleDes.encrypt(cardMasterKey, diversification(atc, RIGHT_MARKER)),
        )
    }

    internal fun diversification(atc: Int, marker: Int): ByteArray =
        atc.toBytes(2) + byteArrayOf(marker.toByte()) + ByteArray(TripleDes.BLOCK_SIZE - 3)
}
