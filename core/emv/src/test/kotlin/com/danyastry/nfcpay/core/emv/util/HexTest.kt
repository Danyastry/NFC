package com.danyastry.nfcpay.core.emv.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class HexTest {

    @Test
    fun `encodes upper case and decodes ignoring spaces`() {
        assertThat(byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte()).toHex()).isEqualTo("007F80FF")
        assertThat("de ad\nBE EF".hexToBytes()).isEqualTo(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()))
    }

    @Test
    fun `rejects odd length and non hex digits`() {
        assertThrows(IllegalArgumentException::class.java) { "ABC".hexToBytes() }
        assertThrows(IllegalArgumentException::class.java) { "ZZ".hexToBytes() }
        assertThat(Hex.isValid("0A")).isTrue()
        assertThat(Hex.isValid("0G")).isFalse()
    }
}
