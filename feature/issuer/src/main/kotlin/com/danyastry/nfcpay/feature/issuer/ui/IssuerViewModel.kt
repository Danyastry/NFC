package com.danyastry.nfcpay.feature.issuer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.nfcpay.core.emv.issuer.IssuerAccount
import com.danyastry.nfcpay.core.nfc.issuer.IssuerLink
import com.danyastry.nfcpay.feature.issuer.logic.IssuerViews
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class IssuerViewModel(
    private val link: IssuerLink,
    issuerMasterKey: ByteArray,
    openingAccount: IssuerAccount,
) : ViewModel() {

    private val keys = IssuerViews.keys(issuerMasterKey, openingAccount)

    val state: StateFlow<IssuerUiState> = combine(link.account, link.events, link.canReplay) { account, events, canReplay ->
        IssuerUiState(
            keys = keys,
            account = account?.let(IssuerViews::account),
            authorizations = events.map(IssuerViews::authorization).asReversed(),
            canReplay = canReplay,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, IssuerUiState(keys, link.account.value?.let(IssuerViews::account)))

    fun onReplay() {
        link.replayLast()
    }

    fun onReset() = link.reset()
}
