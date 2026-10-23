package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.util.toHex

class ResponseApdu(val data: ByteArray, val sw: Int) {

    init {
        require(sw in 0..0xFFFF) { "Status word must be two bytes, got $sw" }
    }

    val isSuccess: Boolean
        get() = sw == StatusWord.SUCCESS

    fun encode(): ByteArray = data + byteArrayOf((sw ushr 8).toByte(), sw.toByte())

    override fun equals(other: Any?): Boolean = other is ResponseApdu && sw == other.sw && data.contentEquals(other.data)

    override fun hashCode(): Int = 31 * data.contentHashCode() + sw

    override fun toString(): String = encode().toHex()

    companion object {
        fun parse(bytes: ByteArray): ResponseApdu {
            if (bytes.size < 2) throw ApduFormatException("A response needs a status word, got ${bytes.size} bytes")
            val sw = ((bytes[bytes.size - 2].toInt() and 0xFF) shl 8) or (bytes.last().toInt() and 0xFF)
            return ResponseApdu(bytes.copyOfRange(0, bytes.size - 2), sw)
        }

        fun status(sw: Int): ResponseApdu = ResponseApdu(ByteArray(0), sw)

        fun success(data: ByteArray): ResponseApdu = ResponseApdu(data, StatusWord.SUCCESS)
    }
}
