package com.danyastry.nfcpay.core.emv.pin

import com.danyastry.nfcpay.core.emv.crypto.TripleDes
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

class IsoPinBlockTest {

    private val aesKey = "00112233445566778899AABBCCDDEEFF".hexToBytes()

    @Test
    fun `format 0 matches the classic published example`() {
        val block = IsoPinBlock.format0("1234", "43219876543210987")

        assertThat(block.toHex()).isEqualTo("0412AC89ABCDEF67")
        assertThat(IsoPinBlock.decode(block, PinBlockFormat.FORMAT_0, "43219876543210987")).isEqualTo("1234")
    }

    @Test
    fun `format 0 uses the rightmost 12 digits without the check digit`() {
        assertThat(IsoPinBlock.legacyPanField("4111111111111111").toHex()).isEqualTo("0000111111111111")
        assertThat(IsoPinBlock.legacyPanField("123456").toHex()).isEqualTo("0000000000012345")
    }

    @Test
    fun `format 0 is sent encrypted under the terminal PIN key`() {
        val encrypted = TripleDes.encrypt("0123456789ABCDEFFEDCBA9876543210".hexToBytes(), IsoPinBlock.format0("1234", "43219876543210987"))

        assertThat(encrypted.toHex()).isEqualTo("C967C8198151A458")
    }

    @Test
    fun `format 1 needs no PAN and has random fill`() {
        val first = IsoPinBlock.format1("123456", Random(1))
        val second = IsoPinBlock.format1("123456", Random(2))

        assertThat(first.toHex()).startsWith("16123456")
        assertThat(first).isNotEqualTo(second)
        assertThat(IsoPinBlock.decode(first, PinBlockFormat.FORMAT_1)).isEqualTo("123456")
    }

    @Test
    fun `format 3 hides equal PINs behind random fill`() {
        val pan = "4111111111111111"
        val first = IsoPinBlock.format3("1234", pan, Random(1))
        val second = IsoPinBlock.format3("1234", pan, Random(2))

        assertThat(first).isNotEqualTo(second)
        assertThat(IsoPinBlock.decode(first, PinBlockFormat.FORMAT_3, pan)).isEqualTo("1234")
        assertThat(IsoPinBlock.decode(second, PinBlockFormat.FORMAT_3, pan)).isEqualTo("1234")
    }

    @Test
    fun `format 4 builds the PIN and PAN fields of ISO 9564-1`() {
        val pinField = IsoPinBlock.format4PinField("1234", FixedNibbles("2F69ADDE2E9E7ACE"))

        assertThat(pinField.toHex()).isEqualTo("441234AAAAAAAAAA2F69ADDE2E9E7ACE")
        assertThat(IsoPinBlock.format4PanField("4111111111111111").toHex()).isEqualTo("44111111111111111000000000000000")
        assertThat(IsoPinBlock.format4PanField("1234567890123456789").toHex()).isEqualTo("71234567890123456789000000000000")
        assertThat(IsoPinBlock.format4PanField("12345").toHex()).isEqualTo("00000000123450000000000000000000")
    }

    @Test
    fun `format 4 enciphers twice with the PAN in between`() {
        val block = IsoPinBlock.encipherFormat4(aesKey, "1234", "4111111111111111", FixedNibbles("2F69ADDE2E9E7ACE"))

        assertThat(block.toHex()).isEqualTo("261BB4FCB189B3C17A55AE1648E0A2FD")
        assertThat(IsoPinBlock.decipherFormat4(aesKey, block, "4111111111111111")).isEqualTo("1234")
    }

    @Test
    fun `format 4 is bound to the PAN and the key`() {
        val block = IsoPinBlock.encipherFormat4(aesKey, "1234", "4111111111111111")

        assertThrows(PinBlockException::class.java) { IsoPinBlock.decipherFormat4(aesKey, block, "4111111111111112") }
        assertThrows(PinBlockException::class.java) { IsoPinBlock.decipherFormat4(ByteArray(16), block, "4111111111111111") }
    }

    @Test
    fun `rejects bad PINs and broken blocks`() {
        listOf("123", "1234567890123", "12a4").forEach { pin ->
            assertThrows(IllegalArgumentException::class.java) { IsoPinBlock.format0(pin, "4111111111111111") }
        }
        assertThrows(PinBlockException::class.java) { IsoPinBlock.decode(IsoPinBlock.format0("1234", "4111111111111111"), PinBlockFormat.FORMAT_1) }
        assertThrows(PinBlockException::class.java) { IsoPinBlock.decode("0412AC89ABCDEF60".hexToBytes(), PinBlockFormat.FORMAT_0, "43219876543210987") }
        assertThrows(PinBlockException::class.java) { IsoPinBlock.decode(ByteArray(16), PinBlockFormat.FORMAT_0, "4111111111111111") }
    }

    private class FixedNibbles(hex: String) : Random() {
        private val digits = hex.map { Character.digit(it, 16) }.iterator()
        override fun nextBits(bitCount: Int): Int = throw UnsupportedOperationException()
        override fun nextInt(until: Int): Int = digits.next()
    }
}
