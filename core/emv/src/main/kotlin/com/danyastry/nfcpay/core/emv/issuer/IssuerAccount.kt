package com.danyastry.nfcpay.core.emv.issuer

import java.time.YearMonth

data class IssuerAccount(
    val pan: String,
    val panSequenceNumber: Int,
    val expiry: YearMonth,
    val balance: Long,
    val lastAtc: Int = NO_TRANSACTIONS,
) {
    companion object {
        const val NO_TRANSACTIONS = -1
    }
}
