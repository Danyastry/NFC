package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class RetailMacTest {

    private val key = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
    private val data = "Now is the time for all ".toByteArray(Charsets.US_ASCII)

    @Test
    fun `matches the ISO 9797-1 algorithm 3 example`() {
        assertThat(RetailMac.computeAlgorithm3(key, data).toHex()).isEqualTo("A1C72E74EA3FA9B6")
    }

    @Test
    fun `padding method 2 adds a full block when the data is aligned`() {
        assertThat(RetailMac.padMethod2(data)).hasLength(32)
        assertThat(RetailMac.padMethod2(ByteArray(5)).toHex()).isEqualTo("0000000000800000")
        assertThat(RetailMac.compute(key, data).toHex()).isEqualTo("E9086230CA3BE796")
    }

    @Test
    fun `needs a double length key and whole blocks`() {
        assertThrows(IllegalArgumentException::class.java) { RetailMac.compute(ByteArray(8), data) }
        assertThrows(IllegalArgumentException::class.java) { RetailMac.computeAlgorithm3(key, ByteArray(7)) }
    }
}
