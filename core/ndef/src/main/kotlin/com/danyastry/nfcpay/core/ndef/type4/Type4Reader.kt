package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
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
        Type4Commands.requireAddressable(Type4Files.NLEN_SIZE, length)
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
        Type4Commands.requireAddressable(Type4Files.NLEN_SIZE, bytes.size)
        val chunkSize = Type4Commands.writeSize(capabilities)
        updateBinary(0, 0.toBytes(2), "UPDATE BINARY - NLEN 0000")
        bytes.toList().chunked(chunkSize).forEachIndexed { index, chunk ->
            val offset = Type4Files.NLEN_SIZE + index * chunkSize
            updateBinary(offset, chunk.toByteArray(), "UPDATE BINARY - offset $offset - ${chunk.size} bytes")
        }
        updateBinary(0, bytes.size.toBytes(2), "UPDATE BINARY - NLEN ${bytes.size.toBytes(2).toHex()}")
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
            expect("SELECT NDEF application", Type4Commands.selectApplication(), TagError.NOT_AN_NDEF_TAG)
            expect("SELECT capability container", Type4Commands.selectFile(Type4Files.CAPABILITY_CONTAINER), TagError.NO_CAPABILITY_CONTAINER)
            val ccBytes = expect(
                "READ BINARY - capability container",
                Type4Commands.readBinary(0, CapabilityContainer.LENGTH),
                TagError.NO_CAPABILITY_CONTAINER,
            )
            val cc = try {
                CapabilityContainer.parse(ccBytes)
            } catch (_: NdefFormatException) {
                throw TagException(TagError.BAD_CAPABILITY_CONTAINER)
            }
            capabilities = cc
            expect("SELECT NDEF file %04X".format(cc.ndefFileId), Type4Commands.selectFile(cc.ndefFileId), TagError.NO_NDEF_FILE)
            return cc
        }

        suspend fun readBinary(start: Int, length: Int, cc: CapabilityContainer): ByteArray {
            var data = ByteArray(0)
            while (data.size < length) {
                val offset = start + data.size
                val chunk = minOf(Type4Commands.readSize(cc), length - data.size)
                val bytes = expect(
                    "READ BINARY - offset $offset - $chunk bytes",
                    Type4Commands.readBinary(offset, chunk),
                    TagError.READ_FAILED,
                )
                if (bytes.isEmpty()) throw TagException(TagError.READ_FAILED)
                data += bytes
            }
            return data.copyOfRange(0, length)
        }

        suspend fun updateBinary(offset: Int, data: ByteArray, label: String) {
            expect(label, Type4Commands.updateBinary(offset, data), TagError.WRITE_FAILED)
        }

        private suspend fun expect(label: String, command: CommandApdu, error: TagError): ByteArray {
            val response = ResponseApdu.parse(transport.transceive(command.encode()))
            exchanges += Exchange(label, command, response)
            if (!response.isSuccess) throw TagException(error)
            return response.data
        }
    }
}
