package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionKeyDerivationTest {

    private val cardKey = "0116F2F17C70E09E2A4ACD43A8C29886".hexToBytes()

    @Test
    fun `builds the diversification blocks from the ATC`() {
        assertThat(SessionKeyDerivation.diversification(0x0001, 0xF0).toHex()).isEqualTo("0001F00000000000")
        assertThat(SessionKeyDerivation.diversification(0x1234, 0x0F).toHex()).isEqualTo("12340F0000000000")
    }

    @Test
    fun `derives the EMV common session key`() {
        assertThat(SessionKeyDerivation.commonSessionKey(cardKey, 1).toHex()).isEqualTo("4963483D5FA7CE6110DBC95B16A7C0A2")
    }

    @Test
    fun `a new transaction gets a new key`() {
        assertThat(SessionKeyDerivation.commonSessionKey(cardKey, 1)).isNotEqualTo(SessionKeyDerivation.commonSessionKey(cardKey, 2))
    }

    @Test
    fun `rejects counters outside two bytes`() {
        assertThrows(IllegalArgumentException::class.java) { SessionKeyDerivation.commonSessionKey(cardKey, 0x10000) }
    }
}
