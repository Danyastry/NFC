package com.danyastry.nfcpay.core.emv.pin

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class VisaPvvTest {

    private val pvk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    @Test
    fun `matches published PVV examples`() {
        assertThat(VisaPvv.generate(pvk, 3, "4524", "1122334455667788")).isEqualTo("4021")
        assertThat(VisaPvv.generate(pvk, 1, "4524", "1122334455667788")).isEqualTo("8523")
        assertThat(VisaPvv.generate(pvk, 2, "1912", "1122334455667788")).isEqualTo("3244")
        assertThat(VisaPvv.generate(pvk, 1, "0570", "1122334455667718")).isEqualTo("3144")
        assertThat(VisaPvv.generate(pvk, 1, "8299", "1122334455667708")).isEqualTo("4422")
    }

    @Test
    fun `builds the transformed security parameter`() {
        assertThat(VisaPvv.transformedSecurityParameter(3, "4524", "1122334455667788")).isEqualTo("33445566778" + "3" + "4524")
    }

    @Test
    fun `only the first four PIN digits count`() {
        assertThat(VisaPvv.generate(pvk, 1, "452499", "1122334455667788")).isEqualTo("8523")
    }

    @Test
    fun `verifies the right PIN only`() {
        assertThat(VisaPvv.verify(pvk, 1, "4524", "1122334455667788", "8523")).isTrue()
        assertThat(VisaPvv.verify(pvk, 1, "4525", "1122334455667788", "8523")).isFalse()
    }

    @Test
    fun `rejects bad inputs`() {
        assertThrows(IllegalArgumentException::class.java) { VisaPvv.generate(pvk, 10, "4524", "1122334455667788") }
        assertThrows(IllegalArgumentException::class.java) { VisaPvv.generate(pvk, 1, "452", "1122334455667788") }
        assertThrows(IllegalArgumentException::class.java) { VisaPvv.generate(pvk, 1, "4524", "11223344556") }
    }
}
