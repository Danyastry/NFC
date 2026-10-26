package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Type4TagTest {

    private val tag = Type4Tag()

    @Test
    fun `files are only reachable after selecting the NDEF application`() {
        assertThat(send(selectFile(0xE103)).sw).isEqualTo(StatusWord.FILE_NOT_FOUND)
        assertThat(send(CommandApdu(0x00, 0xB0, 0x00, 0x00, le = 15)).sw).isEqualTo(Type4Tag.COMMAND_NOT_ALLOWED)

        assertThat(send(selectApp()).sw).isEqualTo(StatusWord.SUCCESS)
        assertThat(send(selectFile(0xE103)).sw).isEqualTo(StatusWord.SUCCESS)
        assertThat(send(CommandApdu(0x00, 0xB0, 0x00, 0x00, le = 15)).data.toHex()).isEqualTo("000F20003B00340406E10408000000")
    }

    @Test
    fun `the capability container cannot be overwritten`() {
        send(selectApp())
        send(selectFile(0xE103))

        assertThat(send(CommandApdu(0x00, 0xD6, 0x00, 0x00, byteArrayOf(0x00))).sw).isEqualTo(StatusWord.SECURITY_STATUS_NOT_SATISFIED)
    }

    @Test
    fun `checks offsets and lengths`() {
        send(selectApp())
        send(selectFile(0xE104))

        assertThat(send(CommandApdu(0x00, 0xB0, 0x09, 0x00, le = 2)).sw).isEqualTo(Type4Tag.WRONG_OFFSET)
        assertThat(send(CommandApdu(0x00, 0xD6, 0x07, 0xFF, ByteArray(2))).sw).isEqualTo(Type4Tag.WRONG_OFFSET)
        assertThat(send(CommandApdu(0x00, 0xD6, 0x00, 0x00, ByteArray(0x35))).sw).isEqualTo(StatusWord.WRONG_LENGTH)
        assertThat(send(CommandApdu(0x00, 0xB0, 0x00, 0x00, le = 0xFF)).data).hasLength(0x3B)
    }

    @Test
    fun `unknown files, instructions and classes are refused`() {
        send(selectApp())

        assertThat(send(selectFile(0xE105)).sw).isEqualTo(StatusWord.FILE_NOT_FOUND)
        assertThat(send(CommandApdu(0x00, 0x20, 0x00, 0x00)).sw).isEqualTo(StatusWord.INS_NOT_SUPPORTED)
        assertThat(send(CommandApdu(0x80, 0xB0, 0x00, 0x00, le = 2)).sw).isEqualTo(StatusWord.CLA_NOT_SUPPORTED)
    }

    @Test
    fun `losing the field forgets the selection`() {
        send(selectApp())
        send(selectFile(0xE104))
        tag.deactivate()

        assertThat(send(CommandApdu(0x00, 0xB0, 0x00, 0x00, le = 2)).sw).isEqualTo(Type4Tag.COMMAND_NOT_ALLOWED)
    }

    private fun selectApp() = CommandApdu(0x00, 0xA4, 0x04, 0x00, Type4Files.NDEF_APPLICATION, le = 256)

    private fun selectFile(id: Int) = CommandApdu(0x00, 0xA4, 0x00, 0x0C, byteArrayOf((id ushr 8).toByte(), id.toByte()))

    private fun send(command: CommandApdu): ResponseApdu = ResponseApdu.parse(tag.process(command.encode()))
}
