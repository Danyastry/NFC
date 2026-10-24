package com.danyastry.nfcpay.core.emv.token

import java.time.YearMonth

data class TokenRecord(
    val token: String,
    val pan: String,
    val panSequenceNumber: Int,
    val expiry: YearMonth,
    val requestorId: String,
    val domain: TokenDomain,
    val status: TokenStatus,
    val paymentAccountReference: String,
)
