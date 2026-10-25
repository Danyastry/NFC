package com.danyastry.nfcpay.feature.tokens.ui

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.danyastry.nfcpay.core.nfc.wallet.PhoneWallet
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.YearMonth
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class TokensViewModelTest {

    private val wallet = PhoneWallet(TokenService(TokenVault(random = Random(5)), DemoCards.issuerMasterKey), tokenExpiry = { YearMonth.of(2029, 10) })
    private lateinit var viewModel: TokensViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TokensViewModel(wallet)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the token, never the full card number`() = runTest {
        assertThat(viewModel.state.value.wallet).isNull()

        viewModel.onAddCard()

        val view = viewModel.state.value.wallet!!
        assertThat(view.token.replace(" ", "")).startsWith("999950")
        assertThat(view.account).isEqualTo("999900******7891")
        assertThat(view.expiry).isEqualTo("10/29")
        assertThat(view.status).isEqualTo("Active")
        assertThat(view.keyIndex).isEqualTo(1)
        assertThat(view.tapsLeft).isEqualTo(3)
    }

    @Test
    fun `follows the lifecycle and keeps deleted tokens in the vault list`() = runTest {
        viewModel.onAddCard()

        viewModel.onSuspend()
        assertThat(viewModel.state.value.wallet!!.isSuspended).isTrue()
        viewModel.onResume()
        viewModel.onReplenish()
        assertThat(viewModel.state.value.wallet!!.keyIndex).isEqualTo(2)
        viewModel.onRemove()

        assertThat(viewModel.state.value.wallet).isNull()
        assertThat(viewModel.state.value.vault.single().status).isEqualTo("Deleted")
    }
}
