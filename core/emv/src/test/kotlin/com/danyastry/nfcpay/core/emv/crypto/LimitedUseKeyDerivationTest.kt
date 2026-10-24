package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class LimitedUseKeyDerivationTest {

    private val tokenMasterKey = "DFFE67C8380DBCD0192A794AF87A3BE9".hexToBytes()

    @Test
    fun `the token key comes from option A over the token number`() {
        val key = IccKeyDerivation.optionA("0123456789ABCDEFFEDCBA9876543210".hexToBytes(), "9999501234567890", 0)

        assertThat(key.toHex()).isEqualTo("DFFE67C8380DBCD0192A794AF87A3BE9")
    }

    @Test
    fun `derives one key per index`() {
        val first = LimitedUseKeyDerivation.derive(tokenMasterKey, 1)
        val second = LimitedUseKeyDerivation.derive(tokenMasterKey, 2)

        assertThat(first.toHex()).isEqualTo("15DA1C19C285D3917A0898D5FDC75785")
        assertThat(KeyCheckValue.of(first)).isEqualTo("38765F")
        assertThat(second.toHex()).isEqualTo("5419681C62891CDF6D43DC98BA3BFE16")
        assertThat(KeyCheckValue.of(second)).isEqualTo("FBBAFE")
        assertThat(DesParity.isOdd(first)).isTrue()
    }

    @Test
    fun `uses markers that differ from the session key`() {
        assertThat(LimitedUseKeyDerivation.diversification(1, 0xA5).toHex()).isEqualTo("0001A50000000000")
        assertThat(LimitedUseKeyDerivation.derive(tokenMasterKey, 1)).isNotEqualTo(SessionKeyDerivation.commonSessionKey(tokenMasterKey, 1))
    }

    @Test
    fun `rejects indexes outside one byte`() {
        assertThrows(IllegalArgumentException::class.java) { LimitedUseKeyDerivation.derive(tokenMasterKey, 0) }
        assertThrows(IllegalArgumentException::class.java) { LimitedUseKeyDerivation.derive(tokenMasterKey, 256) }
    }
}
