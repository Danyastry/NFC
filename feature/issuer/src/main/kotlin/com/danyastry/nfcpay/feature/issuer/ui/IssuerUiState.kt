package com.danyastry.nfcpay.feature.issuer.ui

import com.danyastry.nfcpay.feature.issuer.model.AccountView
import com.danyastry.nfcpay.feature.issuer.model.AuthorizationView
import com.danyastry.nfcpay.feature.issuer.model.KeyHierarchy

data class IssuerUiState(
    val keys: KeyHierarchy,
    val account: AccountView?,
    val authorizations: List<AuthorizationView> = emptyList(),
    val canReplay: Boolean = false,
)
