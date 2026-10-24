package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.issuer.AuthorizationRequest
import com.danyastry.nfcpay.core.emv.issuer.AuthorizationResponse
import com.danyastry.nfcpay.core.emv.issuer.IssuerAccount
import com.danyastry.nfcpay.core.emv.issuer.IssuerAuthorizer
import com.danyastry.nfcpay.core.emv.issuer.IssuerHost
import com.danyastry.nfcpay.core.emv.terminal.OnlineAuthorizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IssuerLink(
    val host: IssuerHost,
    private val openingAccount: IssuerAccount,
    private val clock: () -> Long = System::currentTimeMillis,
    private val logLimit: Int = DEFAULT_LOG_LIMIT,
) {
    private var lastRequest: AuthorizationRequest? = null

    private val _events = MutableStateFlow<List<IssuerEvent>>(emptyList())
    val events: StateFlow<List<IssuerEvent>> = _events.asStateFlow()

    private val _account = MutableStateFlow(current())
    val account: StateFlow<IssuerAccount?> = _account.asStateFlow()

    private val _canReplay = MutableStateFlow(false)
    val canReplay: StateFlow<Boolean> = _canReplay.asStateFlow()

    val authorizer: OnlineAuthorizer = IssuerAuthorizer(host) { request, response ->
        lastRequest = request
        _canReplay.value = true
        record(response, isReplay = false)
    }

    fun replayLast(): AuthorizationResponse? {
        val request = lastRequest ?: return null
        return host.authorize(request).also { record(it, isReplay = true) }
    }

    fun reset() {
        host.reset(listOf(openingAccount))
        lastRequest = null
        _canReplay.value = false
        _events.value = emptyList()
        _account.value = current()
    }

    private fun record(response: AuthorizationResponse, isReplay: Boolean) {
        _events.update { (it + IssuerEvent(response, isReplay, clock())).takeLast(logLimit) }
        _account.value = current()
    }

    private fun current(): IssuerAccount? = host.account(openingAccount.pan, openingAccount.panSequenceNumber)

    companion object {
        const val DEFAULT_LOG_LIMIT = 50
    }
}
