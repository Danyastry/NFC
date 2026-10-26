package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.util.toBytes
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import com.danyastry.nfcpay.core.ndef.message.NdefFormatException

data class CapabilityContainer(
    val mappingVersion: Int = VERSION_2_0,
    val maxReadSize: Int = 0x3B,
    val maxWriteSize: Int = 0x34,
    val ndefFileId: Int = Type4Files.DEFAULT_NDEF_FILE,
    val maxNdefFileSize: Int = 0x0800,
    val readAccess: Int = ACCESS_GRANTED,
    val writeAccess: Int = ACCESS_GRANTED,
) {
    init {
        require(maxReadSize in MIN_TRANSFER..0xFFFF) { "MLe must be at least $MIN_TRANSFER" }
        require(maxWriteSize in 1..0xFFFF) { "MLc must be at least 1" }
        require(maxNdefFileSize in 5..0xFFFE) { "NDEF file size must be 5..65534" }
    }

    val isReadable: Boolean
        get() = readAccess == ACCESS_GRANTED

    val isWritable: Boolean
        get() = writeAccess == ACCESS_GRANTED

    val maxMessageSize: Int
        get() = maxNdefFileSize - Type4Files.NLEN_SIZE

    fun encode(): ByteArray =
        LENGTH.toBytes(2) + byteArrayOf(mappingVersion.toByte()) + maxReadSize.toBytes(2) + maxWriteSize.toBytes(2) +
            byteArrayOf(NDEF_FILE_CONTROL_TAG.toByte(), NDEF_FILE_CONTROL_LENGTH.toByte()) +
            ndefFileId.toBytes(2) + maxNdefFileSize.toBytes(2) + byteArrayOf(readAccess.toByte(), writeAccess.toByte())

    companion object {
        const val LENGTH = 15
        const val VERSION_2_0 = 0x20
        const val ACCESS_GRANTED = 0x00
        const val ACCESS_DENIED = 0xFF
        const val MIN_TRANSFER = 0x0F
        private const val NDEF_FILE_CONTROL_TAG = 0x04
        private const val NDEF_FILE_CONTROL_LENGTH = 0x06

        fun parse(bytes: ByteArray): CapabilityContainer {
            if (bytes.size < LENGTH) throw NdefFormatException("Capability container is ${bytes.size} bytes, needs $LENGTH")
            val ccLength = bytes.copyOfRange(0, 2).toUnsignedInt()
            if (ccLength < LENGTH) throw NdefFormatException("CCLEN $ccLength is too short")
            val version = bytes[2].toInt() and 0xFF
            if (version ushr 4 != 2 && version ushr 4 != 3) throw NdefFormatException("Unsupported mapping version %02X".format(version))
            if (bytes[7].toInt() and 0xFF != NDEF_FILE_CONTROL_TAG) throw NdefFormatException("No NDEF file control TLV")
            return try {
                CapabilityContainer(
                    mappingVersion = version,
                    maxReadSize = bytes.copyOfRange(3, 5).toUnsignedInt(),
                    maxWriteSize = bytes.copyOfRange(5, 7).toUnsignedInt(),
                    ndefFileId = bytes.copyOfRange(9, 11).toUnsignedInt(),
                    maxNdefFileSize = bytes.copyOfRange(11, 13).toUnsignedInt(),
                    readAccess = bytes[13].toInt() and 0xFF,
                    writeAccess = bytes[14].toInt() and 0xFF,
                )
            } catch (error: IllegalArgumentException) {
                throw NdefFormatException(error.message.orEmpty())
            }
        }
    }
}
