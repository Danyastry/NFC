package com.danyastry.nfcpay.feature.issuer.model

data class AccountView(
    val pan: String,
    val balance: String,
    val lastAtc: String,
)
