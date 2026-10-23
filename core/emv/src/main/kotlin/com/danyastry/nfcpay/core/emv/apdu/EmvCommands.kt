package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv

object EmvCommands {

    val PPSE: ByteArray = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    fun select(name: ByteArray): CommandApdu =
        CommandApdu(Instruction.SELECT.cla, Instruction.SELECT.ins, p1 = 0x04, p2 = 0x00, data = name, le = CommandApdu.MAX_SHORT_LE)

    fun getProcessingOptions(pdolData: ByteArray): CommandApdu = CommandApdu(
        Instruction.GET_PROCESSING_OPTIONS.cla,
        Instruction.GET_PROCESSING_OPTIONS.ins,
        p1 = 0x00,
        p2 = 0x00,
        data = Tlv.primitive(EmvTags.COMMAND_TEMPLATE, pdolData).encode(),
        le = CommandApdu.MAX_SHORT_LE,
    )

    fun readRecord(sfi: Int, record: Int): CommandApdu {
        require(sfi in 1..30) { "SFI must be 1..30, got $sfi" }
        require(record in 1..0xFF) { "Record number must be 1..255, got $record" }
        return CommandApdu(Instruction.READ_RECORD.cla, Instruction.READ_RECORD.ins, p1 = record, p2 = (sfi shl 3) or 0x04, le = CommandApdu.MAX_SHORT_LE)
    }

    fun generateAc(type: CryptogramType, cdolData: ByteArray): CommandApdu = CommandApdu(
        Instruction.GENERATE_AC.cla,
        Instruction.GENERATE_AC.ins,
        p1 = type.bits,
        p2 = 0x00,
        data = cdolData,
        le = CommandApdu.MAX_SHORT_LE,
    )

    fun getData(tag: Tag): CommandApdu {
        require(tag.value in 0..0xFFFF) { "GET DATA takes a one or two byte tag, got ${tag.hex}" }
        return CommandApdu(Instruction.GET_DATA.cla, Instruction.GET_DATA.ins, p1 = tag.value ushr 8, p2 = tag.value and 0xFF, le = CommandApdu.MAX_SHORT_LE)
    }
}
