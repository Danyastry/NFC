package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.xor

object IccKeyDerivation {

    private const val DIGITS = 16

    fun optionA(issuerMasterKey: ByteArray, pan: String, panSequenceNumber: Int): ByteArray {
        val y = diversificationData(pan, panSequenceNumber)
        val left = TripleDes.encrypt(issuerMasterKey, y)
        val right = TripleDes.encrypt(issuerMasterKey, y xor ByteArray(TripleDes.BLOCK_SIZE) { 0xFF.toByte() })
        return DesParity.adjust(left + right)
    }

    fun diversificationData(pan: String, panSequenceNumber: Int): ByteArray {
        require(pan.isNotEmpty() && pan.all(Char::isDigit)) { "PAN must be digits" }
        require(panSequenceNumber in 0..99) { "PAN sequence number is two digits, got $panSequenceNumber" }
        val digits = pan + "%02d".format(panSequenceNumber)
        return digits.takeLast(DIGITS).padStart(DIGITS, '0').hexToBytes()
    }
}
