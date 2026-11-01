package com.danyastry.nfcpay.core.emv.card

class IssuerApplicationData(
    val derivationKeyIndex: Int,
    val version: CryptogramVersion,
    cvr: ByteArray,
) {
    private val cvrBytes = cvr.copyOf()

    init {
        require(derivationKeyIndex in 0..0xFF) { "Derivation key index must fit in a byte, got $derivationKeyIndex" }
        require(cvr.size == CVR_LENGTH) { "CVR is $CVR_LENGTH bytes, got ${cvr.size}" }
    }

    val cvr: ByteArray
        get() = cvrBytes.copyOf()

    fun encode(): ByteArray = byteArrayOf((LENGTH - 1).toByte(), derivationKeyIndex.toByte(), version.code.toByte()) + cvrBytes

    companion object {
        const val LENGTH = 7
        private const val CVR_LENGTH = 4
        private const val DKI_OFFSET = 1
        private const val CVN_OFFSET = 2
        private const val CVR_OFFSET = 3

        fun parse(bytes: ByteArray): IssuerApplicationData? {
            if (bytes.size < LENGTH) return null
            val version = CryptogramVersion.of(bytes[CVN_OFFSET].toInt() and 0xFF) ?: return null
            return IssuerApplicationData(bytes[DKI_OFFSET].toInt() and 0xFF, version, bytes.copyOfRange(CVR_OFFSET, LENGTH))
        }
    }
}
