package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.ndef.message.NdefFormatException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CapabilityContainerTest {

    @Test
    fun `encodes the fifteen byte capability container`() {
        assertThat(CapabilityContainer().encode().toHex()).isEqualTo("000F20003B00340406E10408000000")
    }

    @Test
    fun `parses what it encodes and knows read-only tags`() {
        val readOnly = CapabilityContainer(writeAccess = CapabilityContainer.ACCESS_DENIED)

        val parsed = CapabilityContainer.parse(readOnly.encode())

        assertThat(parsed).isEqualTo(readOnly)
        assertThat(parsed.isWritable).isFalse()
        assertThat(parsed.maxMessageSize).isEqualTo(0x0800 - 2)
    }

    @Test
    fun `rejects broken containers`() {
        listOf(
            "000F20003B0034",
            "000A20003B00340406E10408000000",
            "000F10003B00340406E10408000000",
            "000F20003B00340506E10408000000",
            "000F200001003404 06E10408000000".replace(" ", ""),
        ).forEach { hex ->
            assertThrows(NdefFormatException::class.java) { CapabilityContainer.parse(hex.hexToBytes()) }
        }
    }
}
