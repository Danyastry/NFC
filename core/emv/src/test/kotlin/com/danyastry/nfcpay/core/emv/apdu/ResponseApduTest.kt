package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ResponseApduTest {

    @Test
    fun `splits data and status word`() {
        val response = ResponseApdu.parse("6F009000".hexToBytes())

        assertThat(response.data.toHex()).isEqualTo("6F00")
        assertThat(response.sw).isEqualTo(StatusWord.SUCCESS)
        assertThat(response.isSuccess).isTrue()
        assertThat(response.encode().toHex()).isEqualTo("6F009000")
    }

    @Test
    fun `describes status words`() {
        assertThat(StatusWord.describe(0x6A82)).isEqualTo("File or application not found")
        assertThat(StatusWord.describe(0x6110)).isEqualTo("16 more bytes available")
        assertThat(StatusWord.describe(0x63C2)).isEqualTo("Verification failed, 2 tries left")
        assertThat(StatusWord.describe(0x1234)).isEqualTo("Unknown status")
    }

    @Test
    fun `needs at least a status word`() {
        assertThrows(ApduFormatException::class.java) { ResponseApdu.parse(byteArrayOf(0x90.toByte())) }
    }
}
