package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.apdu.ApduFormatException
import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.Instruction
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.util.toBytes

class VirtualCard(val profile: CardProfile, initialAtc: Int = 0) : ApduHandler {

    private val files = CardFiles(profile)
    private var session = CardSession.IDLE

    var atc: Int = initialAtc
        private set

    @Synchronized
    override fun process(command: ByteArray): ByteArray = respond(command).encode()

    @Synchronized
    fun deactivate() {
        session = CardSession.IDLE
    }

    @Synchronized
    fun resetCounter() {
        atc = 0
        session = CardSession.IDLE
    }

    private fun respond(bytes: ByteArray): ResponseApdu {
        val command = try {
            CommandApdu.parse(bytes)
        } catch (_: ApduFormatException) {
            return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        }
        return when (command.instruction) {
            Instruction.SELECT -> select(command)
            Instruction.GET_PROCESSING_OPTIONS -> getProcessingOptions(command)
            Instruction.READ_RECORD -> readRecord(command)
            Instruction.GENERATE_AC -> generateAc(command)
            Instruction.GET_DATA -> getData(command)
            null -> ResponseApdu.status(
                if (command.cla == 0x00 || command.cla == 0x80) StatusWord.INS_NOT_SUPPORTED else StatusWord.CLA_NOT_SUPPORTED,
            )
        }
    }

    private fun select(command: CommandApdu): ResponseApdu {
        if (command.p1 != 0x04) return ResponseApdu.status(StatusWord.INCORRECT_P1_P2)
        return when {
            command.data.contentEquals(EmvCommands.PPSE) -> {
                session = CardSession.IDLE
                ResponseApdu.success(files.ppse())
            }
            command.data.contentEquals(profile.aid) -> {
                session = CardSession.SELECTED
                ResponseApdu.success(files.applicationFci())
            }
            else -> {
                session = CardSession.IDLE
                ResponseApdu.status(StatusWord.FILE_NOT_FOUND)
            }
        }
    }

    private fun getProcessingOptions(command: CommandApdu): ResponseApdu {
        if (session != CardSession.SELECTED) return ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED)
        val template = runCatching { BerTlv.parse(command.data).singleOrNull() }.getOrNull()
        if (template?.tag != EmvTags.COMMAND_TEMPLATE || template.value.size != Dol.totalLength(files.pdol)) {
            return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        }
        if (atc >= MAX_ATC) return ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED)
        atc++
        session = CardSession.PROCESSING
        return ResponseApdu.success(
            Tlv.constructed(
                EmvTags.RESPONSE_FORMAT_2,
                Tlv.primitive(EmvTags.AIP, files.aip),
                Tlv.primitive(EmvTags.AFL, files.afl),
            ).encode(),
        )
    }

    private fun readRecord(command: CommandApdu): ResponseApdu {
        if (command.p2 and 0x07 != 0x04) return ResponseApdu.status(StatusWord.INCORRECT_P1_P2)
        val record = files.record(sfi = command.p2 ushr 3, number = command.p1)
            ?: return ResponseApdu.status(StatusWord.RECORD_NOT_FOUND)
        return ResponseApdu.success(record)
    }

    private fun generateAc(command: CommandApdu): ResponseApdu {
        if (session != CardSession.PROCESSING) return ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED)
        val requested = CryptogramType.fromBits(command.p1) ?: return ResponseApdu.status(StatusWord.INCORRECT_P1_P2)
        if (command.data.size != Dol.totalLength(files.cdol1)) return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        val type = if (requested == CryptogramType.AAC) CryptogramType.AAC else CryptogramType.ARQC
        val atcBytes = atc.toBytes(2)
        val key = CardCryptogram.key(profile.masterKey, profile.cryptogramVersion, atc)
        val cryptogram = CardCryptogram.compute(key, command.data, files.aip, atc, CardCryptogram.cvr(type))
        session = CardSession.COMPLETED
        return ResponseApdu.success(
            Tlv.constructed(
                EmvTags.RESPONSE_FORMAT_2,
                Tlv.primitive(EmvTags.CRYPTOGRAM_INFORMATION_DATA, byteArrayOf(type.bits.toByte())),
                Tlv.primitive(EmvTags.ATC, atcBytes),
                Tlv.primitive(EmvTags.APPLICATION_CRYPTOGRAM, cryptogram),
                Tlv.primitive(EmvTags.ISSUER_APPLICATION_DATA, CardCryptogram.issuerApplicationData(type, profile.cryptogramVersion)),
            ).encode(),
        )
    }

    private fun getData(command: CommandApdu): ResponseApdu {
        val tag = (command.p1 shl 8) or command.p2
        if (tag != EmvTags.ATC.value) return ResponseApdu.status(StatusWord.REFERENCED_DATA_NOT_FOUND)
        return ResponseApdu.success(Tlv.primitive(EmvTags.ATC, atc.toBytes(2)).encode())
    }

    companion object {
        const val MAX_ATC = 0xFFFF
    }
}
