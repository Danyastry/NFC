package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CommandApduTest {

    @Test
    fun `encodes the four ISO 7816 cases`() {
        assertThat(CommandApdu(0x00, 0xA4, 0x04, 0x00).encode().toHex()).isEqualTo("00A40400")
        assertThat(CommandApdu(0x00, 0xB2, 0x01, 0x0C, le = 256).encode().toHex()).isEqualTo("00B2010C00")
        assertThat(CommandApdu(0x00, 0xA4, 0x04, 0x00, "A0000000031010".hexToBytes()).encode().toHex())
            .isEqualTo("00A4040007A0000000031010")
        assertThat(CommandApdu(0x00, 0xA4, 0x04, 0x00, "A0000000031010".hexToBytes(), le = 256).encode().toHex())
            .isEqualTo("00A4040007A000000003101000")
    }

    @Test
    fun `parses what it encodes`() {
        listOf("00A40400", "00B2010C00", "00A4040007A0000000031010", "80A8000002830000", "80CA9F3610").forEach { hex ->
            assertThat(CommandApdu.parse(hex.hexToBytes()).encode().toHex()).isEqualTo(hex)
        }
        assertThat(CommandApdu.parse("80CA9F3610".hexToBytes()).le).isEqualTo(0x10)
        assertThat(CommandApdu.parse("00B2010C00".hexToBytes()).le).isEqualTo(256)
    }

    @Test
    fun `recognises EMV instructions`() {
        assertThat(EmvCommands.select(EmvCommands.PPSE).instruction).isEqualTo(Instruction.SELECT)
        assertThat(EmvCommands.readRecord(1, 2).encode().toHex()).isEqualTo("00B2020C00")
        assertThat(EmvCommands.getProcessingOptions(ByteArray(0)).encode().toHex()).isEqualTo("80A80000028300" + "00")
        assertThat(CommandApdu(0x00, 0xAE, 0, 0).instruction).isNull()
    }

    @Test
    fun `rejects bodies that do not match Lc`() {
        assertThrows(ApduFormatException::class.java) { CommandApdu.parse("00A4".hexToBytes()) }
        assertThrows(ApduFormatException::class.java) { CommandApdu.parse("00A4040005A000".hexToBytes()) }
    }
}
