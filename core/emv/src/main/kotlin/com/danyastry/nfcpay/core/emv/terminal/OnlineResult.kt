package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.response.ResponseCode

data class OnlineResult(
    val responseCode: String,
    val reachedIssuer: Boolean,
    val finalCryptogram: Cryptogram?,
) {
    val isApproved: Boolean
        get() = finalCryptogram?.type?.let { it == CryptogramType.TC } ?: (responseCode == ResponseCode.APPROVED.code)

    val cardOverruledIssuer: Boolean
        get() = responseCode == ResponseCode.APPROVED.code && finalCryptogram?.type == CryptogramType.AAC
}
