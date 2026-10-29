package com.danyastry.nfcpay.feature.card.logic

import com.danyastry.nfcpay.core.emv.apdu.ApduFormatException
import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.Instruction
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.card.CardProfile
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.nfc.hce.ServedApdu
import com.danyastry.nfcpay.feature.card.model.CardDetails
import com.danyastry.nfcpay.feature.card.model.ServedEntry
import java.time.format.DateTimeFormatter

object ServedEntries {

    private val EXPIRY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yy")

    fun map(served: ServedApdu): ServedEntry {
        val response = runCatching { ResponseApdu.parse(served.response) }.getOrNull()
        return ServedEntry(
            label = label(served.command),
            source = served.source,
            command = served.command.toHex(),
            response = served.response.toHex(),
            sw = response?.sw,
            timeMillis = served.timeMillis,
        )
    }

    fun label(bytes: ByteArray): String {
        val command = try {
            CommandApdu.parse(bytes)
        } catch (_: ApduFormatException) {
            return "Malformed command"
        }
        return when (command.instruction) {
            Instruction.SELECT -> if (command.data.contentEquals(EmvCommands.PPSE)) "SELECT PPSE" else "SELECT ${command.data.toHex()}"
            Instruction.READ_RECORD -> "READ RECORD - SFI ${command.p2 ushr 3} - record ${command.p1}"
            Instruction.GENERATE_AC -> "GENERATE AC - ${CryptogramType.fromBits(command.p1)?.label ?: "RFU"}"
            Instruction.GET_DATA -> "GET DATA %02X%02X".format(command.p1, command.p2)
            Instruction.GET_PROCESSING_OPTIONS -> Instruction.GET_PROCESSING_OPTIONS.label
            null -> "CLA %02X INS %02X".format(command.cla, command.ins)
        }
    }

    fun details(profile: CardProfile): CardDetails = CardDetails(
        label = profile.label,
        pan = SensitiveData.groupPan(profile.pan),
        expiry = profile.expiry.format(EXPIRY_FORMAT),
        cardholderName = profile.cardholderName,
        aid = profile.aid.toHex(),
    )
}
