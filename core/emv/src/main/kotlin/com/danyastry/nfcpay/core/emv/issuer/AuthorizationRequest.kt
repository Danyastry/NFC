package com.danyastry.nfcpay.core.emv.issuer

import java.time.LocalDate

class AuthorizationRequest(
    val pan: String,
    val panSequenceNumber: Int,
    val field55: ByteArray,
    val date: LocalDate,
)
