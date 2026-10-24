package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.issuer.AuthorizationResponse

data class IssuerEvent(
    val response: AuthorizationResponse,
    val isReplay: Boolean,
    val timeMillis: Long,
)
