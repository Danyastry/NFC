package com.danyastry.nfcpay.core.emv.tlv

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt

@JvmInline
value class Tag(val value: Int) {

    init {
        require(value in 0x01..0xFFFFFF) { "EMV tags are one to three bytes, got $value" }
    }

    val bytes: ByteArray
        get() = value.toBytes(
            when {
                value <= 0xFF -> 1
                value <= 0xFFFF -> 2
                else -> 3
            },
        )

    val isConstructed: Boolean
        get() = bytes[0].toInt() and CONSTRUCTED_BIT != 0

    val tagClass: TagClass
        get() = TagClass.entries[(bytes[0].toInt() and 0xC0) ushr 6]

    val hex: String
        get() = bytes.toHex()

    override fun toString(): String = hex

    companion object {
        const val CONSTRUCTED_BIT = 0x20
        const val MORE_BYTES_MASK = 0x1F
        const val NEXT_BYTE_BIT = 0x80

        fun of(hex: String): Tag = Tag(hex.hexToBytes().toUnsignedInt())
    }
}
