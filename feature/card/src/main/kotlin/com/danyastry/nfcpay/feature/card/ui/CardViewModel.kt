package com.danyastry.nfcpay.feature.card.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.nfcpay.core.nfc.hce.CardHost
import com.danyastry.nfcpay.feature.card.logic.ServedEntries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CardViewModel(private val host: CardHost) : ViewModel() {

    private val details = ServedEntries.details(host.profile)

    val state: StateFlow<CardUiState> = combine(host.atc, host.log) { atc, log ->
        CardUiState(details = details, atc = atc, log = log.map(ServedEntries::map).asReversed())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CardUiState(details = details, atc = host.atc.value))

    fun onResetCounter() = host.resetCounter()

    fun onClearLog() = host.clearLog()
}
