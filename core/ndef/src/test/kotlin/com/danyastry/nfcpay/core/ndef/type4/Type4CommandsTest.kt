package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class Type4CommandsTest {

    @Test
    fun `offsets travel in a 15 bit P1 P2 and decode back`() {
        listOf(0, 1, 0x00FF, 0x0100, 0x1234, Type4Commands.MAX_OFFSET).forEach { offset ->
            val read = Type4Commands.readBinary(offset, 16)
            val update = Type4Commands.updateBinary(offset, ByteArray(4))

            assertThat(read.p1 and 0x80).isEqualTo(0)
            assertThat(Type4Commands.offsetOf(read)).isEqualTo(offset)
            assertThat(Type4Commands.offsetOf(update)).isEqualTo(offset)
        }
    }

    @Test
    fun `the top P1 bit means short file addressing and has no offset`() {
        assertThat(Type4Commands.offsetOf(CommandApdu(0x00, 0xB0, 0x81, 0x00, le = 1))).isNull()
    }

    @Test
    fun `builders refuse what a short command cannot carry`() {
        assertThrows(IllegalArgumentException::class.java) { Type4Commands.readBinary(0x8000, 1) }
        assertThrows(IllegalArgumentException::class.java) { Type4Commands.readBinary(0, 257) }
        assertThrows(IllegalArgumentException::class.java) { Type4Commands.updateBinary(0x8000, ByteArray(1)) }
        assertThrows(IllegalArgumentException::class.java) { Type4Commands.updateBinary(0, ByteArray(256)) }
        assertThrows(IllegalArgumentException::class.java) { Type4Commands.updateBinary(0, ByteArray(0)) }
    }

    @Test
    fun `chunk sizes never exceed the short APDU limits whatever the tag advertises`() {
        val generous = CapabilityContainer(maxReadSize = 0xFFFF, maxWriteSize = 0xFFFF)
        val small = CapabilityContainer(maxReadSize = 0x20, maxWriteSize = 0x10)

        assertThat(Type4Commands.readSize(generous)).isEqualTo(256)
        assertThat(Type4Commands.writeSize(generous)).isEqualTo(255)
        assertThat(Type4Commands.readSize(small)).isEqualTo(0x20)
        assertThat(Type4Commands.writeSize(small)).isEqualTo(0x10)
    }

    @Test
    fun `the message must end within the first 32 KB`() {
        Type4Commands.requireAddressable(2, Type4Commands.MAX_OFFSET - 1)
        Type4Commands.requireAddressable(2, 0)

        val error = assertThrows(TagException::class.java) { Type4Commands.requireAddressable(2, Type4Commands.MAX_OFFSET) }
        assertThat(error.error).isEqualTo(TagError.BEYOND_SHORT_OFFSETS)
    }

    @Test
    fun `select commands match the NFC Forum procedure`() {
        assertThat(Type4Commands.selectApplication().encode().toHex()).isEqualTo("00A4040007D276000085010100")
        assertThat(Type4Commands.selectFile(Type4Files.CAPABILITY_CONTAINER).encode().toHex()).isEqualTo("00A4000C02E103")
    }
}
