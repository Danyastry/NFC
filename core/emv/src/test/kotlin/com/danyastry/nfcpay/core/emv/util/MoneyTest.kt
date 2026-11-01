package com.danyastry.nfcpay.core.emv.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats minor units with two decimals`() {
        assertThat(Money.format(250_00)).isEqualTo("250.00")
        assertThat(Money.format(1250)).isEqualTo("12.50")
        assertThat(Money.format(7)).isEqualTo("0.07")
        assertThat(Money.format(0)).isEqualTo("0.00")
    }
}
