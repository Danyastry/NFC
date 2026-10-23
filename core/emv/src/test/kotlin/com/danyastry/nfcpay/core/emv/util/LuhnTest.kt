package com.danyastry.nfcpay.core.emv.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LuhnTest {

    @Test
    fun `accepts known valid numbers`() {
        assertThat(Luhn.isValid("79927398713")).isTrue()
        assertThat(Luhn.isValid("4111111111111111")).isTrue()
        assertThat(Luhn.isValid("9999001234567891")).isTrue()
    }

    @Test
    fun `rejects a changed digit or non digits`() {
        assertThat(Luhn.isValid("79927398710")).isFalse()
        assertThat(Luhn.isValid("4111111111111112")).isFalse()
        assertThat(Luhn.isValid("4111 1111 1111 1111")).isFalse()
    }

    @Test
    fun `computes the check digit`() {
        assertThat(Luhn.checkDigit("7992739871")).isEqualTo('3')
        assertThat(Luhn.checkDigit("999900123456789")).isEqualTo('1')
    }
}
