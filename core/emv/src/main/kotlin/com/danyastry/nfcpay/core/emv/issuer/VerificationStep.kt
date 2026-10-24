package com.danyastry.nfcpay.core.emv.issuer

data class VerificationStep(
    val title: String,
    val detail: String,
    val passed: Boolean,
)
