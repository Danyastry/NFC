package com.danyastry.nfcpay.core.emv.terminal

class OnlineResponse(
    val responseCode: String,
    val issuerAuthenticationData: ByteArray?,
) {
    init {
        require(responseCode.length == 2) { "Response codes are two characters, got '$responseCode'" }
    }

    val isApproved: Boolean
        get() = responseCode == APPROVED

    companion object {
        const val APPROVED = "00"
        const val UNABLE_TO_GO_ONLINE = "Z3"
    }
}
