package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.issuer.AuthorizationResponse
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.issuer.IssuerHost
import com.danyastry.nfcpay.core.emv.terminal.OnlineAuthorizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IssuerLink(val host: IssuerHost, private val logLimit: Int = DEFAULT_LOG_LIMIT) {

    private val _responses = MutableStateFlow<List<AuthorizationResponse>>(emptyList())
    val responses: StateFlow<List<AuthorizationResponse>> = _responses.asStateFlow()

    val authorizer: OnlineAuthorizer = IssuerAuthorizer(host) { response ->
        _responses.update { (it + response).takeLast(logLimit) }
    }

    companion object {
        const val DEFAULT_LOG_LIMIT = 50
    }
}
