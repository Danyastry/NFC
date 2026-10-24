package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ArpcTest {

    private val sessionKey = "4963483D5FA7CE6110DBC95B16A7C0A2".hexToBytes()

    @Test
    fun `method 1 encrypts the ARQC xored with the response code`() {
        val arpc = Arpc.method1(sessionKey, "1122334455667788".hexToBytes(), "00".toByteArray(Charsets.US_ASCII))

        assertThat(arpc.toHex()).isEqualTo("B101BDCF0505EF40")
    }

    @Test
    fun `a different answer gives a different ARPC`() {
        val approve = Arpc.method1(sessionKey, "1122334455667788".hexToBytes(), "00".toByteArray())
        val decline = Arpc.method1(sessionKey, "1122334455667788".hexToBytes(), "05".toByteArray())

        assertThat(approve).isNotEqualTo(decline)
    }

    @Test
    fun `checks input sizes`() {
        assertThrows(IllegalArgumentException::class.java) { Arpc.method1(sessionKey, ByteArray(4), "00".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { Arpc.method1(sessionKey, ByteArray(8), "000".toByteArray()) }
    }
}
