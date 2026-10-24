package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.crypto.LimitedUseKeyDerivation
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.YearMonth
import kotlin.random.Random

class TokenServiceTest {

    private val service = TokenService(TokenVault(random = Random(1)), DemoCards.issuerMasterKey, usesPerKey = 3)

    @Test
    fun `provisions a contactless token with its first key`() {
        val wallet = service.provisionWallet(DemoCards.lab.pan, 1, DemoCards.walletRequestorId, YearMonth.of(2029, 10))

        assertThat(wallet.token.domain).isEqualTo(TokenDomain.CONTACTLESS)
        assertThat(wallet.token.pan).isEqualTo(DemoCards.lab.pan)
        assertThat(wallet.firstKey.index).isEqualTo(1)
        assertThat(wallet.firstKey.usesLeft).isEqualTo(3)
        val expected = LimitedUseKeyDerivation.derive(service.tokenMasterKey(wallet.token.token), 1)
        assertThat(wallet.firstKey.key).isEqualTo(expected)
    }

    @Test
    fun `a new key retires the old one`() {
        val token = service.provisionWallet(DemoCards.lab.pan, 1, DemoCards.walletRequestorId, YearMonth.of(2029, 10)).token.token

        val next = service.replenish(token)

        assertThat(next.index).isEqualTo(2)
        assertThat(service.activeKeyIndex(token)).isEqualTo(2)
        assertThat(service.keyFor(token, 2)).isEqualTo(next.key)
        assertThat(service.keyFor(token, 1)).isNull()
    }

    @Test
    fun `token keys differ from the card key`() {
        val token = service.provisionWallet(DemoCards.lab.pan, 1, DemoCards.walletRequestorId, YearMonth.of(2029, 10)).token.token

        assertThat(service.tokenMasterKey(token)).isNotEqualTo(DemoCards.lab.masterKey)
    }

    @Test
    fun `suspended and unknown tokens get no new keys`() {
        val token = service.provisionWallet(DemoCards.lab.pan, 1, DemoCards.walletRequestorId, YearMonth.of(2029, 10)).token.token
        service.vault.suspend(token)

        assertThrows(IllegalStateException::class.java) { service.replenish(token) }
        assertThrows(IllegalArgumentException::class.java) { service.replenish("9999500000000000") }
    }
}
