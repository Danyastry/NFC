package com.danyastry.nfcpay.core.emv.dukpt

import com.danyastry.nfcpay.core.emv.pin.IsoPinBlock
import kotlin.random.Random

class DukptTerminal(initialKey: ByteArray, private val initialKeyId: ByteArray) {

    private val registers = arrayOfNulls<ByteArray>(REGISTERS)
    private var counter = 1L

    init {
        require(initialKey.size == AesDukpt.KEY_LENGTH) { "AES-128 initial keys only" }
        val derivationId = initialKeyId.copyOfRange(4, KeySerialNumber.INITIAL_KEY_ID_LENGTH)
        for (bit in 0 until REGISTERS) registers[bit] = AesDukpt.childKey(initialKey, derivationId, 1L shl bit)
    }

    val nextCounter: Long
        @Synchronized get() = counter

    val isExhausted: Boolean
        @Synchronized get() = counter > KeySerialNumber.MAX_COUNTER

    val storedKeys: Int
        @Synchronized get() = registers.count { it != null }

    @Synchronized
    fun nextTransaction(): DukptTransaction {
        check(!isExhausted) { "This device has used every transaction counter" }
        val ksn = KeySerialNumber(initialKeyId, counter)
        val low = java.lang.Long.numberOfTrailingZeros(counter)
        val key = checkNotNull(registers[low]) { "Future key for bit $low is missing" }
        val canExtend = java.lang.Long.bitCount(counter) < KeySerialNumber.MAX_SET_BITS
        if (canExtend) {
            for (bit in 0 until low) registers[bit] = AesDukpt.childKey(key, ksn.derivationId, counter or (1L shl bit))
        }
        registers[low] = null
        counter = if (canExtend) counter + 1 else counter + (1L shl low)
        return DukptTransaction(ksn, key)
    }

    fun encryptPin(pin: String, pan: String, random: Random = Random.Default): EncryptedPin {
        val transaction = nextTransaction()
        val pinKey = AesDukpt.workingKey(transaction.key, DukptKeyUsage.PIN_ENCRYPTION, transaction.ksn)
        return EncryptedPin(transaction.ksn, IsoPinBlock.encipherFormat4(pinKey, pin, pan, random))
    }

    private companion object {
        const val REGISTERS = 32
    }
}
