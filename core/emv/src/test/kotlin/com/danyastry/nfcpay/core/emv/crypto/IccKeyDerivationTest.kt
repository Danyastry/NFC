package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class IccKeyDerivationTest {

    private val imk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    @Test
    fun `takes the rightmost 16 digits of PAN and sequence number`() {
        assertThat(IccKeyDerivation.diversificationData("9999001234567891", 1).toHex()).isEqualTo("9900123456789101")
        assertThat(IccKeyDerivation.diversificationData("4111111111111111", 0).toHex()).isEqualTo("1111111111111100")
    }

    @Test
    fun `pads short account numbers on the left`() {
        assertThat(IccKeyDerivation.diversificationData("123456789012", 3).toHex()).isEqualTo("0012345678901203")
    }

    @Test
    fun `derives the card master key with option A`() {
        val key = IccKeyDerivation.optionA(imk, "9999001234567891", 1)

        assertThat(key.toHex()).isEqualTo("0116F2F17C70E09E2A4ACD43A8C29886")
        assertThat(DesParity.isOdd(key)).isTrue()
        assertThat(KeyCheckValue.of(key)).isEqualTo("06DC71")
    }

    @Test
    fun `every card gets its own key`() {
        val first = IccKeyDerivation.optionA(imk, "9999001234567891", 1)
        val second = IccKeyDerivation.optionA(imk, "9999001234567891", 2)

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `rejects bad inputs`() {
        assertThrows(IllegalArgumentException::class.java) { IccKeyDerivation.diversificationData("4111-1111", 0) }
        assertThrows(IllegalArgumentException::class.java) { IccKeyDerivation.diversificationData("4111111111111111", 100) }
    }
}
