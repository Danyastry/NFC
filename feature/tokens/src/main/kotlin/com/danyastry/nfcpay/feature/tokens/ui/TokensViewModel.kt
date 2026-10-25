package com.danyastry.nfcpay.feature.tokens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.nfcpay.core.nfc.wallet.PhoneWallet
import com.danyastry.nfcpay.feature.tokens.logic.TokenViews
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TokensViewModel(private val wallet: PhoneWallet) : ViewModel() {

    val state: StateFlow<TokensUiState> = combine(wallet.state, wallet.tokens) { current, tokens ->
        TokensUiState(wallet = current?.let(TokenViews::wallet), vault = tokens.map(TokenViews::vault).asReversed())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TokensUiState())

    fun onAddCard() = wallet.addCard()

    fun onReplenish() = wallet.replenish()

    fun onSuspend() = wallet.suspend()

    fun onResume() = wallet.resume()

    fun onRemove() = wallet.remove()
}
