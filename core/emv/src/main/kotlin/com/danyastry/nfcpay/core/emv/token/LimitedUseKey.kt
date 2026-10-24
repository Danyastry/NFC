package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.crypto.KeyCheckValue

class LimitedUseKey(
    val index: Int,
    val key: ByteArray,
    val usesLeft: Int,
) {
    init {
        require(index in 1..0xFF) { "Key index must be 1..255, got $index" }
        require(key.size == 16) { "Limited-use keys are double-length DES keys" }
        require(usesLeft >= 0) { "Uses left cannot be negative" }
    }

    val checkValue: String
        get() = KeyCheckValue.of(key)

    fun used(): LimitedUseKey = LimitedUseKey(index, key, usesLeft - 1)

    override fun toString(): String = "LimitedUseKey(index=$index, kcv=$checkValue, usesLeft=$usesLeft)"
}
