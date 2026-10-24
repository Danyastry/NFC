package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class WalletKeysTest {

    private val first = LimitedUseKey(index = 1, key = ByteArray(16) { 1 }, usesLeft = 2)

    @Test
    fun `each transaction uses up one tap of the key`() {
        val keys = WalletKeys(first)

        assertThat(keys.version).isEqualTo(CryptogramVersion.LIMITED_USE_KEY)
        assertThat(keys.startTransaction(atc = 1)).isTrue()
        assertThat(keys.cryptogramKey()).isEqualTo(first.key)
        assertThat(keys.startTransaction(atc = 2)).isTrue()
        assertThat(keys.needsReplenishment).isTrue()
        assertThat(keys.startTransaction(atc = 3)).isFalse()
    }

    @Test
    fun `a new key from the cloud brings taps back`() {
        val keys = WalletKeys(first)
        repeat(2) { keys.startTransaction(it + 1) }

        keys.replenish(LimitedUseKey(index = 2, key = ByteArray(16) { 2 }, usesLeft = 2))

        assertThat(keys.startTransaction(atc = 3)).isTrue()
        assertThat(keys.derivationKeyIndex).isEqualTo(2)
        assertThat(keys.cryptogramKey()).isEqualTo(ByteArray(16) { 2 })
    }

    @Test
    fun `a transaction keeps its key when new keys arrive mid-tap`() {
        val keys = WalletKeys(first)
        keys.startTransaction(atc = 1)

        keys.replenish(LimitedUseKey(index = 2, key = ByteArray(16) { 2 }, usesLeft = 2))

        assertThat(keys.cryptogramKey()).isEqualTo(first.key)
        assertThat(keys.derivationKeyIndex).isEqualTo(1)
    }

    @Test
    fun `refuses old keys and keys before a tap`() {
        val keys = WalletKeys(first)

        assertThrows(IllegalArgumentException::class.java) { keys.replenish(LimitedUseKey(1, ByteArray(16), 2)) }
        assertThrows(IllegalStateException::class.java) { keys.cryptogramKey() }
    }

    @Test
    fun `never prints the key`() {
        assertThat(first.toString()).doesNotContain("0101")
        assertThat(first.toString()).contains("kcv=")
    }
}
