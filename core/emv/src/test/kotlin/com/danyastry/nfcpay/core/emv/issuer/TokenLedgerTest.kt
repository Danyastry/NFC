package com.danyastry.nfcpay.core.emv.issuer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TokenLedgerTest {

    private val ledger = TokenLedger()

    @Test
    fun `tracks the last counter and the uses of each key per token`() {
        ledger.record("T1", atc = 4, keyIndex = 1)
        ledger.record("T1", atc = 5, keyIndex = 1)
        ledger.record("T1", atc = 6, keyIndex = 2)

        assertThat(ledger.lastAtc("T1")).isEqualTo(6)
        assertThat(ledger.uses("T1", 1)).isEqualTo(2)
        assertThat(ledger.uses("T1", 2)).isEqualTo(1)
        assertThat(ledger.lastAtc("T2")).isEqualTo(IssuerAccount.NO_TRANSACTIONS)
        assertThat(ledger.uses("T2", 1)).isEqualTo(0)
    }

    @Test
    fun `clear forgets everything`() {
        ledger.record("T1", atc = 4, keyIndex = 1)

        ledger.clear()

        assertThat(ledger.lastAtc("T1")).isEqualTo(IssuerAccount.NO_TRANSACTIONS)
        assertThat(ledger.uses("T1", 1)).isEqualTo(0)
    }
}
