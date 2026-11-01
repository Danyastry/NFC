package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CommandDescriberTest {

    @Test
    fun `labels the commands a payment terminal sends`() {
        assertThat(CommandDescriber.describe(EmvCommands.select(EmvCommands.PPSE))).isEqualTo("SELECT PPSE")
        assertThat(CommandDescriber.describe(EmvCommands.select(DemoCards.lab.aid))).isEqualTo("SELECT F04E464350415901")
        assertThat(CommandDescriber.describe(EmvCommands.getProcessingOptions(ByteArray(2)))).isEqualTo("GET PROCESSING OPTIONS")
        assertThat(CommandDescriber.describe(EmvCommands.readRecord(1, 2))).isEqualTo("READ RECORD - SFI 1 - record 2")
        assertThat(CommandDescriber.describe(EmvCommands.generateAc(CryptogramType.ARQC, ByteArray(1)))).isEqualTo("GENERATE AC - ARQC")
        assertThat(CommandDescriber.describe(EmvCommands.generateAc(CryptogramType.TC, ByteArray(1)))).isEqualTo("GENERATE AC - TC")
        assertThat(CommandDescriber.describe(EmvCommands.getData(EmvTags.ATC))).isEqualTo("GET DATA 9F36")
    }

    @Test
    fun `labels the file commands an NDEF reader sends`() {
        assertThat(CommandDescriber.describe(CommandApdu(0x00, 0xA4, 0x00, 0x0C, "E103".hexToBytes()))).isEqualTo("SELECT file E103")
        assertThat(CommandDescriber.describe(CommandApdu(0x00, 0xB0, 0x01, 0x02, le = 59))).isEqualTo("READ BINARY - offset 258 - 59 bytes")
        assertThat(CommandDescriber.describe(CommandApdu(0x00, 0xD6, 0x00, 0x02, ByteArray(16)))).isEqualTo("UPDATE BINARY - offset 2 - 16 bytes")
        assertThat(CommandDescriber.describe(CommandApdu(0x00, 0xC0, 0x00, 0x00, le = 16))).isEqualTo("GET RESPONSE")
    }

    @Test
    fun `unknown and malformed commands are still labelled`() {
        assertThat(CommandDescriber.describe("00200080".hexToBytes())).isEqualTo("CLA 00 INS 20")
        assertThat(CommandDescriber.describe("80B00000".hexToBytes())).isEqualTo("CLA 80 INS B0")
        assertThat(CommandDescriber.describe("00".hexToBytes())).isEqualTo("Malformed command")
    }
}
