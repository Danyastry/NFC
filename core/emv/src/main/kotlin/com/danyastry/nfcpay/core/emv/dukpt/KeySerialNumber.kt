package com.danyastry.nfcpay.core.emv.dukpt

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt

class KeySerialNumber(val initialKeyId: ByteArray, val counter: Long) {

    init {
        require(initialKeyId.size == INITIAL_KEY_ID_LENGTH) { "Initial key ID is $INITIAL_KEY_ID_LENGTH bytes, got ${initialKeyId.size}" }
        require(counter in 0..MAX_COUNTER) { "Transaction counter is 32 bits, got $counter" }
    }

    val derivationId: ByteArray
        get() = initialKeyId.copyOfRange(4, INITIAL_KEY_ID_LENGTH)

    val isUsable: Boolean
        get() = counter > 0 && java.lang.Long.bitCount(counter) <= MAX_SET_BITS

    fun encode(): ByteArray = initialKeyId + counter.toInt().toBytes(4)

    override fun equals(other: Any?): Boolean =
        other is KeySerialNumber && counter == other.counter && initialKeyId.contentEquals(other.initialKeyId)

    override fun hashCode(): Int = 31 * initialKeyId.contentHashCode() + counter.hashCode()

    override fun toString(): String = encode().toHex()

    companion object {
        const val INITIAL_KEY_ID_LENGTH = 8
        const val LENGTH = 12
        const val MAX_SET_BITS = 16
        const val MAX_COUNTER = 0xFFFFFFFFL

        fun parse(bytes: ByteArray): KeySerialNumber {
            require(bytes.size == LENGTH) { "AES DUKPT KSNs are $LENGTH bytes, got ${bytes.size}" }
            return KeySerialNumber(bytes.copyOfRange(0, INITIAL_KEY_ID_LENGTH), bytes.copyOfRange(8, LENGTH).toUnsignedInt().toLong() and MAX_COUNTER)
        }

        fun parse(hex: String): KeySerialNumber = parse(hex.hexToBytes())
    }
}
