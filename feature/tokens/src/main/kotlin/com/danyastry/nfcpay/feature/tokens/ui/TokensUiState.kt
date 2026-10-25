package com.danyastry.nfcpay.feature.tokens.ui

import com.danyastry.nfcpay.feature.tokens.model.VaultEntry
import com.danyastry.nfcpay.feature.tokens.model.WalletView

data class TokensUiState(
    val wallet: WalletView? = null,
    val vault: List<VaultEntry> = emptyList(),
)
