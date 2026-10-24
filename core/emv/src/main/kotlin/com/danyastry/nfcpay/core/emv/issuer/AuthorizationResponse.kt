package com.danyastry.nfcpay.core.emv.issuer

data class AuthorizationResponse(
    val responseCode: ResponseCode,
    val declineReason: DeclineReason?,
    val amount: Long?,
    val atc: Int?,
    val arpc: String?,
    val issuerAuthenticationData: String?,
    val steps: List<VerificationStep>,
) {
    val isApproved: Boolean
        get() = responseCode == ResponseCode.APPROVED
}
