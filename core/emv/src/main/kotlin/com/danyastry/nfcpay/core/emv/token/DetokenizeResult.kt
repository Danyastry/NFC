package com.danyastry.nfcpay.core.emv.token

sealed interface DetokenizeResult {
    data class Found(val record: TokenRecord) : DetokenizeResult
    data class Refused(val reason: TokenRefusal) : DetokenizeResult
}
