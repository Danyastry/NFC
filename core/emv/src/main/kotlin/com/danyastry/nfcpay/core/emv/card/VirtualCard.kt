package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.apdu.ApduFormatException
import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.Instruction
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.crypto.Arpc
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.util.toBytes

class VirtualCard(
    val profile: CardProfile,
    initialAtc: Int = 0,
    private val keys: CardKeys = StaticCardKeys(profile.masterKey, profile.cryptogramVersion),
) : ApduHandler {

    private val files = CardFiles(profile)
    private var session = CardSession.IDLE
    private var firstAc: FirstAc? = null

    var lastIssuerAuthentication: IssuerAuthentication = IssuerAuthentication.NOT_PERFORMED
        private set

    var atc: Int = initialAtc
        private set

    @Synchronized
    override fun process(command: ByteArray): ByteArray = respond(command).encode()

    @Synchronized
    fun deactivate() {
        session = CardSession.IDLE
        firstAc = null
    }

    @Synchronized
    fun resetCounter() {
        atc = 0
        session = CardSession.IDLE
        firstAc = null
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
        if (!keys.startTransaction(atc + 1)) return ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED)
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

    private fun generateAc(command: CommandApdu): ResponseApdu = when (session) {
        CardSession.PROCESSING -> firstGenerateAc(command)
        CardSession.AWAITING_ISSUER -> secondGenerateAc(command)
        else -> ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED)
    }

    private fun firstGenerateAc(command: CommandApdu): ResponseApdu {
        val requested = CryptogramType.fromBits(command.p1) ?: return ResponseApdu.status(StatusWord.INCORRECT_P1_P2)
        if (command.data.size != Dol.totalLength(files.cdol1)) return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        val type = if (requested == CryptogramType.AAC) CryptogramType.AAC else CryptogramType.ARQC
        val key = keys.cryptogramKey()
        val cvr = CardCryptogram.cvr(type)
        val cryptogram = CardCryptogram.compute(key, command.data, files.aip, atc, cvr)
        if (type == CryptogramType.ARQC) {
            firstAc = FirstAc(key, command.data, cryptogram)
            session = CardSession.AWAITING_ISSUER
        } else {
            session = CardSession.COMPLETED
        }
        return cryptogramResponse(type, cryptogram, CardCryptogram.issuerApplicationData(keys.version, cvr, keys.derivationKeyIndex))
    }

    private fun secondGenerateAc(command: CommandApdu): ResponseApdu {
        val first = firstAc ?: return ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED)
        val requested = CryptogramType.fromBits(command.p1)
        if (requested == null || requested == CryptogramType.ARQC) return ResponseApdu.status(StatusWord.INCORRECT_P1_P2)
        if (command.data.size != Dol.totalLength(files.cdol2)) return ResponseApdu.status(StatusWord.WRONG_LENGTH)
        val values = Dol.split(files.cdol2, command.data)
        val responseCode = values.getValue(EmvTags.AUTHORISATION_RESPONSE_CODE)
        val authentication = authenticateIssuer(first, values.getValue(EmvTags.ISSUER_AUTHENTICATION_DATA), responseCode)
        val approve = requested == CryptogramType.TC &&
            authentication == IssuerAuthentication.PASSED &&
            responseCode.contentEquals(APPROVED_CODE)
        val type = if (approve) CryptogramType.TC else CryptogramType.AAC
        val cvr = CardCryptogram.secondCvr(type, authentication)
        val cryptogram = CardCryptogram.compute(first.key, first.cdolData + command.data, files.aip, atc, cvr)
        lastIssuerAuthentication = authentication
        firstAc = null
        session = CardSession.COMPLETED
        return cryptogramResponse(type, cryptogram, CardCryptogram.issuerApplicationData(keys.version, cvr, keys.derivationKeyIndex))
    }

    private fun authenticateIssuer(first: FirstAc, issuerData: ByteArray, responseCode: ByteArray): IssuerAuthentication {
        if (issuerData.all { it == 0.toByte() }) return IssuerAuthentication.NOT_PERFORMED
        val arpc = issuerData.copyOfRange(0, 8)
        val signedCode = issuerData.copyOfRange(8, 10)
        val expected = Arpc.method1(first.key, first.cryptogram, signedCode)
        return if (expected.contentEquals(arpc) && signedCode.contentEquals(responseCode)) {
            IssuerAuthentication.PASSED
        } else {
            IssuerAuthentication.FAILED
        }
    }

    private fun cryptogramResponse(type: CryptogramType, cryptogram: ByteArray, iad: ByteArray): ResponseApdu =
        ResponseApdu.success(
            Tlv.constructed(
                EmvTags.RESPONSE_FORMAT_2,
                Tlv.primitive(EmvTags.CRYPTOGRAM_INFORMATION_DATA, byteArrayOf(type.bits.toByte())),
                Tlv.primitive(EmvTags.ATC, atc.toBytes(2)),
                Tlv.primitive(EmvTags.APPLICATION_CRYPTOGRAM, cryptogram),
                Tlv.primitive(EmvTags.ISSUER_APPLICATION_DATA, iad),
            ).encode(),
        )

    private fun getData(command: CommandApdu): ResponseApdu {
        val tag = (command.p1 shl 8) or command.p2
        if (tag != EmvTags.ATC.value) return ResponseApdu.status(StatusWord.REFERENCED_DATA_NOT_FOUND)
        return ResponseApdu.success(Tlv.primitive(EmvTags.ATC, atc.toBytes(2)).encode())
    }

    companion object {
        const val MAX_ATC = 0xFFFF
        private val APPROVED_CODE = "00".toByteArray(Charsets.US_ASCII)
    }
}
