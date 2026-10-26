package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.terminal.Exchange
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.core.emv.util.toBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import com.danyastry.nfcpay.core.ndef.message.NdefFormatException
import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import java.io.IOException

class Type4Reader {

    suspend fun read(transport: CardTransport): TagSession = Session(transport).run {
        val capabilities = open()
        if (!capabilities.isReadable) throw TagException(TagError.READ_FAILED)
        val lengthBytes = readBinary(0, Type4Files.NLEN_SIZE, capabilities)
        val length = lengthBytes.toUnsignedInt()
        if (length == 0) throw TagException(TagError.EMPTY)
        if (length > capabilities.maxMessageSize) throw TagException(TagError.MALFORMED_MESSAGE)
        val bytes = readBinary(Type4Files.NLEN_SIZE, length, capabilities)
        val message = try {
            NdefMessage.parse(bytes)
        } catch (_: NdefFormatException) {
            throw TagException(TagError.MALFORMED_MESSAGE)
        }
        capabilities to message
    }

    suspend fun write(transport: CardTransport, message: NdefMessage): TagSession = Session(transport).run {
        val bytes = message.encode()
        val capabilities = open()
        if (!capabilities.isWritable) throw TagException(TagError.READ_ONLY)
        if (bytes.size > capabilities.maxMessageSize) throw TagException(TagError.TOO_LARGE)
        updateBinary(0, 0.toBytes(2), "UPDATE BINARY · NLEN 0000")
        bytes.toList().chunked(capabilities.maxWriteSize).forEachIndexed { index, chunk ->
            val offset = Type4Files.NLEN_SIZE + index * capabilities.maxWriteSize
            updateBinary(offset, chunk.toByteArray(), "UPDATE BINARY · offset $offset · ${chunk.size} bytes")
        }
        updateBinary(0, bytes.size.toBytes(2), "UPDATE BINARY · NLEN ${bytes.size.toBytes(2).toHex()}")
        capabilities to message
    }

    private class Session(private val transport: CardTransport) {
        val exchanges = mutableListOf<Exchange>()
        private var capabilities: CapabilityContainer? = null

        suspend fun run(block: suspend Session.() -> Pair<CapabilityContainer, NdefMessage>): TagSession = try {
            val (cc, message) = block()
            TagSession(exchanges.toList(), cc, message, error = null)
        } catch (failure: TagException) {
            TagSession(exchanges.toList(), capabilities, message = null, error = failure.error)
        } catch (_: IOException) {
            TagSession(exchanges.toList(), capabilities, message = null, error = TagError.TAG_LOST)
        }

        suspend fun open(): CapabilityContainer {
            expect("SELECT NDEF application", EmvCommands.select(Type4Files.NDEF_APPLICATION), TagError.NOT_AN_NDEF_TAG)
            expect("SELECT capability container", selectFile(Type4Files.CAPABILITY_CONTAINER), TagError.NO_CAPABILITY_CONTAINER)
            val ccBytes = expect(
                "READ BINARY · capability container",
                CommandApdu(0x00, Type4Files.INS_READ_BINARY, 0x00, 0x00, le = CapabilityContainer.LENGTH),
                TagError.NO_CAPABILITY_CONTAINER,
            )
            val cc = try {
                CapabilityContainer.parse(ccBytes)
            } catch (_: NdefFormatException) {
                throw TagException(TagError.BAD_CAPABILITY_CONTAINER)
            }
            capabilities = cc
            expect("SELECT NDEF file %04X".format(cc.ndefFileId), selectFile(cc.ndefFileId), TagError.NO_NDEF_FILE)
            return cc
        }

        suspend fun readBinary(start: Int, length: Int, cc: CapabilityContainer): ByteArray {
            var data = ByteArray(0)
            while (data.size < length) {
                val offset = start + data.size
                val chunk = minOf(cc.maxReadSize, length - data.size, CommandApdu.MAX_SHORT_LE)
                val bytes = expect(
                    "READ BINARY · offset $offset · $chunk bytes",
                    CommandApdu(0x00, Type4Files.INS_READ_BINARY, offset ushr 8, offset and 0xFF, le = chunk),
                    TagError.READ_FAILED,
                )
                if (bytes.isEmpty()) throw TagException(TagError.READ_FAILED)
                data += bytes
            }
            return data.copyOfRange(0, length)
        }

        suspend fun updateBinary(offset: Int, data: ByteArray, label: String) {
            expect(label, CommandApdu(0x00, Type4Files.INS_UPDATE_BINARY, offset ushr 8, offset and 0xFF, data), TagError.WRITE_FAILED)
        }

        private suspend fun expect(label: String, command: CommandApdu, error: TagError): ByteArray {
            val response = ResponseApdu.parse(transport.transceive(command.encode()))
            exchanges += Exchange(label, command, response)
            if (!response.isSuccess) throw TagException(error)
            return response.data
        }

        private fun selectFile(id: Int) = CommandApdu(0x00, Type4Files.INS_SELECT, 0x00, 0x0C, id.toBytes(2))
    }
}
