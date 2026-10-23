package com.danyastry.nfcpay.feature.terminal.logic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AmountInputTest {

    @Test
    fun `reads whole and decimal amounts as minor units`() {
        assertThat(AmountInput.parse("12")).isEqualTo(1200)
        assertThat(AmountInput.parse("12.5")).isEqualTo(1250)
        assertThat(AmountInput.parse("12,05")).isEqualTo(1205)
        assertThat(AmountInput.parse(" 0.01 ")).isEqualTo(1)
    }

    @Test
    fun `rejects empty, zero, negative and over-precise amounts`() {
        listOf("", "0", "0.00", "-1", "1.234", "abc", "1.2.3", "12345678901").forEach {
            assertThat(AmountInput.parse(it)).isNull()
        }
    }

    @Test
    fun `formats minor units`() {
        assertThat(AmountInput.format(1250)).isEqualTo("12.50")
        assertThat(AmountInput.format(7)).isEqualTo("0.07")
    }
}
