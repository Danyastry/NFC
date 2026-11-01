package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.util.toHex

object CommandDescriber {

    fun describe(bytes: ByteArray): String = try {
        describe(CommandApdu.parse(bytes))
    } catch (_: ApduFormatException) {
        "Malformed command"
    }

    fun describe(command: CommandApdu): String = when (command.instruction) {
        Instruction.SELECT -> select(command)
        Instruction.READ_RECORD -> "READ RECORD - SFI ${command.p2 ushr 3} - record ${command.p1}"
        Instruction.GENERATE_AC -> "GENERATE AC - ${CryptogramType.fromBits(command.p1)?.label ?: "RFU"}"
        Instruction.GET_DATA -> "GET DATA %02X%02X".format(command.p1, command.p2)
        Instruction.GET_PROCESSING_OPTIONS -> Instruction.GET_PROCESSING_OPTIONS.label
        null -> interindustry(command)
    }

    private fun select(command: CommandApdu): String = when {
        command.p1 == SELECT_BY_NAME && command.data.contentEquals(EmvCommands.PPSE) -> "SELECT PPSE"
        command.p1 == SELECT_BY_FILE_ID && command.data.size == 2 -> "SELECT file ${command.data.toHex()}"
        else -> "SELECT ${command.data.toHex()}"
    }

    private fun interindustry(command: CommandApdu): String = when {
        command.cla != 0x00 -> unknown(command)
        command.ins == READ_BINARY -> "READ BINARY - offset ${offset(command)} - ${command.le ?: 0} bytes"
        command.ins == UPDATE_BINARY -> "UPDATE BINARY - offset ${offset(command)} - ${command.data.size} bytes"
        command.ins == GET_RESPONSE -> "GET RESPONSE"
        else -> unknown(command)
    }

    private fun offset(command: CommandApdu): Int = (command.p1 shl 8) or command.p2

    private fun unknown(command: CommandApdu): String = "CLA %02X INS %02X".format(command.cla, command.ins)

    private const val SELECT_BY_NAME = 0x04
    private const val SELECT_BY_FILE_ID = 0x00
    private const val READ_BINARY = 0xB0
    private const val UPDATE_BINARY = 0xD6
    private const val GET_RESPONSE = 0xC0
}
