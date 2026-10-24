package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.CryptogramType

data class OnlineResult(
    val responseCode: String,
    val reachedIssuer: Boolean,
    val finalCryptogram: Cryptogram?,
) {
    val isApproved: Boolean
        get() = finalCryptogram?.type?.let { it == CryptogramType.TC } ?: (responseCode == OnlineResponse.APPROVED)

    val cardOverruledIssuer: Boolean
        get() = responseCode == OnlineResponse.APPROVED && finalCryptogram?.type == CryptogramType.AAC
}
