package com.danyastry.nfcpay.core.emv.dukpt

import com.danyastry.nfcpay.core.emv.crypto.Aes
import com.danyastry.nfcpay.core.emv.util.toBytes

object AesDukpt {

    const val KEY_LENGTH = 16
    private const val VERSION = 0x01
    private const val FIRST_BLOCK = 0x01
    private const val AES_128 = 0x0002
    private const val KEY_BITS = 128

    fun initialKey(baseDerivationKey: ByteArray, initialKeyId: ByteArray): ByteArray {
        require(baseDerivationKey.size == KEY_LENGTH) { "This lab supports AES-128 base derivation keys" }
        require(initialKeyId.size == KeySerialNumber.INITIAL_KEY_ID_LENGTH) { "Initial key ID is 8 bytes" }
        return Aes.encrypt(baseDerivationKey, derivationData(DukptKeyUsage.INITIAL_KEY, initialKeyId))
    }

    fun transactionKey(initialKey: ByteArray, ksn: KeySerialNumber): ByteArray {
        require(ksn.isUsable) { "Counter ${ksn.counter} is zero or has more than ${KeySerialNumber.MAX_SET_BITS} bits set" }
        var key = initialKey
        var working = 0L
        var mask = 1L shl 31
        while (mask != 0L) {
            if (ksn.counter and mask != 0L) {
                working = working or mask
                key = childKey(key, ksn.derivationId, working)
            }
            mask = mask ushr 1
        }
        return key
    }

    fun workingKey(transactionKey: ByteArray, usage: DukptKeyUsage, ksn: KeySerialNumber): ByteArray {
        require(usage != DukptKeyUsage.INITIAL_KEY && usage != DukptKeyUsage.KEY_DERIVATION) { "${usage.label} is not a working key" }
        return Aes.encrypt(transactionKey, derivationData(usage, ksn.derivationId + ksn.counter.toInt().toBytes(4)))
    }

    fun hostWorkingKey(baseDerivationKey: ByteArray, ksn: KeySerialNumber, usage: DukptKeyUsage): ByteArray =
        workingKey(transactionKey(initialKey(baseDerivationKey, ksn.initialKeyId), ksn), usage, ksn)

    internal fun childKey(parent: ByteArray, derivationId: ByteArray, counter: Long): ByteArray =
        Aes.encrypt(parent, derivationData(DukptKeyUsage.KEY_DERIVATION, derivationId + counter.toInt().toBytes(4)))

    internal fun derivationData(usage: DukptKeyUsage, ksnData: ByteArray): ByteArray {
        require(ksnData.size == 8) { "Derivation data carries 8 bytes of KSN data" }
        return byteArrayOf(VERSION.toByte(), FIRST_BLOCK.toByte()) +
            usage.code.toBytes(2) + AES_128.toBytes(2) + KEY_BITS.toBytes(2) + ksnData
    }
}
