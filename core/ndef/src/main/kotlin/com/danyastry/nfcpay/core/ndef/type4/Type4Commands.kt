package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.util.toBytes

object Type4Commands {

    const val MAX_OFFSET = 0x7FFF
    const val MAX_READ = CommandApdu.MAX_SHORT_LE
    const val MAX_WRITE = CommandApdu.MAX_SHORT_DATA

    fun selectApplication(): CommandApdu = EmvCommands.select(Type4Files.NDEF_APPLICATION)

    fun selectFile(id: Int): CommandApdu = CommandApdu(0x00, Type4Files.INS_SELECT, 0x00, 0x0C, id.toBytes(2))

    fun readBinary(offset: Int, length: Int): CommandApdu {
        require(offset in 0..MAX_OFFSET) { "READ BINARY offset must be 0..$MAX_OFFSET, got $offset" }
        require(length in 1..MAX_READ) { "READ BINARY length must be 1..$MAX_READ, got $length" }
        return CommandApdu(0x00, Type4Files.INS_READ_BINARY, offset ushr 8, offset and 0xFF, le = length)
    }

    fun updateBinary(offset: Int, data: ByteArray): CommandApdu {
        require(offset in 0..MAX_OFFSET) { "UPDATE BINARY offset must be 0..$MAX_OFFSET, got $offset" }
        require(data.size in 1..MAX_WRITE) { "UPDATE BINARY carries 1..$MAX_WRITE bytes, got ${data.size}" }
        return CommandApdu(0x00, Type4Files.INS_UPDATE_BINARY, offset ushr 8, offset and 0xFF, data)
    }

    fun offsetOf(command: CommandApdu): Int? =
        if (command.p1 and SHORT_FILE_IDENTIFIER_BIT != 0) null else (command.p1 shl 8) or command.p2

    fun readSize(capabilities: CapabilityContainer): Int = minOf(capabilities.maxReadSize, MAX_READ)

    fun writeSize(capabilities: CapabilityContainer): Int = minOf(capabilities.maxWriteSize, MAX_WRITE)

    fun requireAddressable(start: Int, length: Int) {
        if (length > 0 && start + length - 1 > MAX_OFFSET) throw TagException(TagError.BEYOND_SHORT_OFFSETS)
    }

    private const val SHORT_FILE_IDENTIFIER_BIT = 0x80
}
