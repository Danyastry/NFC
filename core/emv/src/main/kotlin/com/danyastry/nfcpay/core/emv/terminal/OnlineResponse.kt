package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.response.ResponseCode

class OnlineResponse(
    val responseCode: String,
    val issuerAuthenticationData: ByteArray?,
) {
    init {
        require(responseCode.length == 2) { "Response codes are two characters, got '$responseCode'" }
    }

    val isApproved: Boolean
        get() = responseCode == ResponseCode.APPROVED.code
}
