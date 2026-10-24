package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.util.Luhn
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class TokenVaultTest {

    private val pan = "9999001234567891"
    private val requestor = "40010030273"
    private val date = LocalDate.of(2026, 10, 24)
    private val vault = TokenVault(random = Random(42))

    @Test
    fun `issues a Luhn valid token from the token BIN`() {
        val record = vault.provision(pan, 1, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10))

        assertThat(record.token).hasLength(16)
        assertThat(record.token).startsWith("999950")
        assertThat(Luhn.isValid(record.token)).isTrue()
        assertThat(record.token).isNotEqualTo(pan)
        assertThat(record.status).isEqualTo(TokenStatus.ACTIVE)
        assertThat(vault.isToken(record.token)).isTrue()
        assertThat(vault.isToken(pan)).isFalse()
    }

    @Test
    fun `every token is unique`() {
        val tokens = (1..500).map { vault.provision(pan, 1, requestor, TokenDomain.ECOMMERCE, YearMonth.of(2029, 10)).token }

        assertThat(tokens).containsNoDuplicates()
        assertThat(vault.tokensFor(pan)).hasSize(500)
    }

    @Test
    fun `all tokens of one account share its payment account reference`() {
        val phone = vault.provision(pan, 1, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10))
        val shop = vault.provision(pan, 1, "40010075001", TokenDomain.ECOMMERCE, YearMonth.of(2029, 10))
        val other = vault.provision("4111111111111111", 0, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10))

        assertThat(phone.paymentAccountReference).isEqualTo(shop.paymentAccountReference)
        assertThat(phone.paymentAccountReference).isNotEqualTo(other.paymentAccountReference)
        assertThat(phone.paymentAccountReference).hasLength(29)
        assertThat(phone.paymentAccountReference).startsWith("Q999")
        assertThat(phone.paymentAccountReference).matches("[0-9A-Z]{29}")
    }

    @Test
    fun `maps a token back to the account`() {
        val record = vault.provision(pan, 1, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10))

        val result = vault.detokenize(record.token, TokenDomain.CONTACTLESS, date)

        assertThat(result).isEqualTo(DetokenizeResult.Found(record))
    }

    @Test
    fun `a token only works in its own channel`() {
        val record = vault.provision(pan, 1, requestor, TokenDomain.ECOMMERCE, YearMonth.of(2029, 10))

        assertThat(vault.detokenize(record.token, TokenDomain.CONTACTLESS, date)).isEqualTo(DetokenizeResult.Refused(TokenRefusal.WRONG_DOMAIN))
    }

    @Test
    fun `refuses unknown, expired, suspended and deleted tokens`() {
        val expired = vault.provision(pan, 1, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2026, 9))
        val token = vault.provision(pan, 1, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10)).token

        assertThat(vault.detokenize("9999509999999999", TokenDomain.CONTACTLESS, date)).isEqualTo(DetokenizeResult.Refused(TokenRefusal.UNKNOWN_TOKEN))
        assertThat(vault.detokenize(expired.token, TokenDomain.CONTACTLESS, date)).isEqualTo(DetokenizeResult.Refused(TokenRefusal.EXPIRED))
        vault.suspend(token)
        assertThat(vault.detokenize(token, TokenDomain.CONTACTLESS, date)).isEqualTo(DetokenizeResult.Refused(TokenRefusal.SUSPENDED))
        vault.resume(token)
        assertThat(vault.detokenize(token, TokenDomain.CONTACTLESS, date)).isInstanceOf(DetokenizeResult.Found::class.java)
        vault.delete(token)
        assertThat(vault.detokenize(token, TokenDomain.CONTACTLESS, date)).isEqualTo(DetokenizeResult.Refused(TokenRefusal.DELETED))
    }

    @Test
    fun `only allows valid lifecycle moves`() {
        val token = vault.provision(pan, 1, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10)).token

        assertThrows(IllegalStateException::class.java) { vault.resume(token) }
        vault.delete(token)
        assertThrows(IllegalStateException::class.java) { vault.suspend(token) }
        assertThrows(IllegalArgumentException::class.java) { vault.suspend("0000") }
    }

    @Test
    fun `checks what it is given`() {
        assertThrows(IllegalArgumentException::class.java) { vault.provision("4111111111111112", 0, requestor, TokenDomain.CONTACTLESS, YearMonth.of(2029, 10)) }
        assertThrows(IllegalArgumentException::class.java) { vault.provision(pan, 1, "123", TokenDomain.CONTACTLESS, YearMonth.of(2029, 10)) }
        assertThrows(IllegalArgumentException::class.java) { TokenVault(tokenBin = "12AB56") }
        assertThrows(IllegalArgumentException::class.java) { TokenVault(binControllerId = "q99") }
    }
}
