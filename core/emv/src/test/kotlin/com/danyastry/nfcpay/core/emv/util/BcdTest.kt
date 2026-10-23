package com.danyastry.nfcpay.core.emv.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BcdTest {

    @Test
    fun `amounts are twelve digit packed BCD`() {
        assertThat(Bcd.encode(1234, 6).toHex()).isEqualTo("000000001234")
        assertThat(Bcd.encode(840, 2).toHex()).isEqualTo("0840")
        assertThat(Bcd.decodeNumber("000000001234".hexToBytes())).isEqualTo(1234)
    }

    @Test
    fun `rejects values that do not fit or are not digits`() {
        assertThrows(IllegalArgumentException::class.java) { Bcd.encode(1000, 1) }
        assertThrows(IllegalArgumentException::class.java) { Bcd.encode(-1, 2) }
        assertThrows(IllegalArgumentException::class.java) { Bcd.encodeDigits("12A4", 2) }
    }
}
