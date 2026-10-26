package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.ApduFormatException
import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.card.ApduHandler
import com.danyastry.nfcpay.core.emv.util.toBytes
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import com.danyastry.nfcpay.core.ndef.message.NdefMessage

class Type4Tag(
    val capabilities: CapabilityContainer = CapabilityContainer(),
    initial: NdefMessage? = null,
) : ApduHandler {

    private val ndefFile = ByteArray(capabilities.maxNdefFileSize)
    private var applicationSelected = false
    private var selectedFile: Int? = null

    init {
        initial?.let { store(it.encode()) }
    }

    val message: NdefMessage?
        @Synchronized get() {
            val length = ndefFile.copyOfRange(0, Type4Files.NLEN_SIZE).toUnsignedInt()
            if (length == 0 || length > capabilities.maxMessageSize) return null
            return runCatching { NdefMessage.parse(ndefFile.copyOfRange(Type4Files.NLEN_SIZE, Type4Files.NLEN_SIZE + length)) }.getOrNull()
        }

    @Synchronized
    override fun process(command: ByteArray): ByteArray = respond(command).encode()

    @Synchronized
    fun deactivate() {
        applicationSelected = false
        selectedFile = null
    }

    private fun respond(bytes: ByteArray): ResponseApdu {
        val command = try {
            CommandApdu.parse(bytes)
        } catch (_: ApduFormatException) {
            return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        }
        if (command.cla != 0x00) return ResponseApdu.status(StatusWord.CLA_NOT_SUPPORTED)
        return when (command.ins) {
            Type4Files.INS_SELECT -> select(command)
            Type4Files.INS_READ_BINARY -> readBinary(command)
            Type4Files.INS_UPDATE_BINARY -> updateBinary(command)
            else -> ResponseApdu.status(StatusWord.INS_NOT_SUPPORTED)
        }
    }

    private fun select(command: CommandApdu): ResponseApdu = when {
        command.p1 == 0x04 && command.data.contentEquals(Type4Files.NDEF_APPLICATION) -> {
            applicationSelected = true
            selectedFile = null
            ResponseApdu.status(StatusWord.SUCCESS)
        }
        command.p1 == 0x04 -> {
            applicationSelected = false
            ResponseApdu.status(StatusWord.FILE_NOT_FOUND)
        }
        command.p1 == 0x00 && command.p2 == 0x0C && applicationSelected && command.data.size == 2 -> {
            val file = command.data.toUnsignedInt()
            if (file == Type4Files.CAPABILITY_CONTAINER || file == capabilities.ndefFileId) {
                selectedFile = file
                ResponseApdu.status(StatusWord.SUCCESS)
            } else {
                ResponseApdu.status(StatusWord.FILE_NOT_FOUND)
            }
        }
        command.p1 == 0x00 -> ResponseApdu.status(StatusWord.FILE_NOT_FOUND)
        else -> ResponseApdu.status(StatusWord.INCORRECT_P1_P2)
    }

    private fun readBinary(command: CommandApdu): ResponseApdu {
        val file = selectedFile ?: return ResponseApdu.status(COMMAND_NOT_ALLOWED)
        val content = if (file == Type4Files.CAPABILITY_CONTAINER) {
            capabilities.encode()
        } else {
            if (!capabilities.isReadable) return ResponseApdu.status(StatusWord.SECURITY_STATUS_NOT_SATISFIED)
            ndefFile
        }
        val offset = (command.p1 shl 8) or command.p2
        if (offset > content.size) return ResponseApdu.status(WRONG_OFFSET)
        val length = minOf(command.le ?: 0, capabilities.maxReadSize, content.size - offset)
        return ResponseApdu.success(content.copyOfRange(offset, offset + length))
    }

    private fun updateBinary(command: CommandApdu): ResponseApdu {
        val file = selectedFile ?: return ResponseApdu.status(COMMAND_NOT_ALLOWED)
        if (file == Type4Files.CAPABILITY_CONTAINER || !capabilities.isWritable) {
            return ResponseApdu.status(StatusWord.SECURITY_STATUS_NOT_SATISFIED)
        }
        val offset = (command.p1 shl 8) or command.p2
        if (command.data.isEmpty() || command.data.size > capabilities.maxWriteSize) return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        if (offset + command.data.size > ndefFile.size) return ResponseApdu.status(WRONG_OFFSET)
        command.data.copyInto(ndefFile, offset)
        return ResponseApdu.status(StatusWord.SUCCESS)
    }

    private fun store(message: ByteArray) {
        require(message.size <= capabilities.maxMessageSize) { "Message does not fit the NDEF file" }
        message.size.toBytes(2).copyInto(ndefFile, 0)
        message.copyInto(ndefFile, Type4Files.NLEN_SIZE)
    }

    companion object {
        const val COMMAND_NOT_ALLOWED = 0x6986
        const val WRONG_OFFSET = 0x6B00
    }
}
