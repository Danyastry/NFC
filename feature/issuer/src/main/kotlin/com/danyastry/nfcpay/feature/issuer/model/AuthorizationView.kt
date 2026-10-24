package com.danyastry.nfcpay.feature.issuer.model

import com.danyastry.nfcpay.core.emv.issuer.VerificationStep

data class AuthorizationView(
    val responseCode: String,
    val description: String,
    val isApproved: Boolean,
    val isReplay: Boolean,
    val amount: String?,
    val atc: Int?,
    val steps: List<VerificationStep>,
    val timeMillis: Long,
)
