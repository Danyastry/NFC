package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.IssuerApplicationData
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.YearMonth
import kotlin.random.Random

class IssuerCredentialTest {

    private val account = DemoIssuer.account()
    private val service = TokenService(TokenVault(random = Random(9)), DemoCards.issuerMasterKey, usesPerKey = 2)
    private val wallet = service.provisionWallet(account.pan, account.panSequenceNumber, DemoCards.walletRequestorId, YearMonth.of(2029, 10))
    private val ledger = TokenLedger()
    private val ignore: (String, String) -> Unit = { _, _ -> }

    @Test
    fun `a plastic card binds the PIN to its own PAN and counts on the account`() {
        val card = CardCredential(account, DemoCards.issuerMasterKey)

        assertThat(card.pinBlockPan).isEqualTo(account.pan)
        assertThat(card.accepts(CryptogramVersion.SESSION_KEY)).isTrue()
        assertThat(card.accepts(CryptogramVersion.LIMITED_USE_KEY)).isFalse()
        assertThat(card.counted(atc = 7).lastAtc).isEqualTo(7)
    }

    @Test
    fun `a token binds the PIN to the token, uses limited-use keys and counts in the ledger`() {
        val token = TokenCredential(account, wallet.token, service, ledger)
        val iad = IssuerApplicationData(1, CryptogramVersion.LIMITED_USE_KEY, CardCryptogram.cvr(CryptogramType.ARQC))

        val key = token.cryptogramKey(iad, atc = 3, step = ignore)
        val counted = token.counted(atc = 3)

        assertThat(token.pinBlockPan).isEqualTo(wallet.token.token)
        assertThat(token.accepts(CryptogramVersion.LIMITED_USE_KEY)).isTrue()
        assertThat(token.accepts(CryptogramVersion.CARD_KEY)).isFalse()
        assertThat(key).isEqualTo(wallet.firstKey.key)
        assertThat(counted).isEqualTo(account)
        assertThat(ledger.lastAtc(wallet.token.token)).isEqualTo(3)
        assertThat(ledger.uses(wallet.token.token, 1)).isEqualTo(1)
    }

    @Test
    fun `a token refuses retired and used-up keys`() {
        val iadFor = { index: Int -> IssuerApplicationData(index, CryptogramVersion.LIMITED_USE_KEY, CardCryptogram.cvr(CryptogramType.ARQC)) }
        repeat(2) { ledger.record(wallet.token.token, atc = it, keyIndex = 1) }

        val exhausted = assertThrows(AuthorizationDecline::class.java) {
            TokenCredential(account, wallet.token, service, ledger).cryptogramKey(iadFor(1), atc = 9, step = ignore)
        }
        val retired = assertThrows(AuthorizationDecline::class.java) {
            TokenCredential(account, wallet.token, service, ledger).cryptogramKey(iadFor(2), atc = 9, step = ignore)
        }

        assertThat(exhausted.reason).isEqualTo(DeclineReason.KEY_EXHAUSTED)
        assertThat(retired.reason).isEqualTo(DeclineReason.KEY_RETIRED)
    }
}
