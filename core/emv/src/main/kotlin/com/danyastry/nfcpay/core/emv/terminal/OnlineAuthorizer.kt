package com.danyastry.nfcpay.core.emv.terminal

fun interface OnlineAuthorizer {
    suspend fun authorize(request: OnlineRequest): OnlineResponse?
}
