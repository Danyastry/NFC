package com.danyastry.nfcpay.feature.tokens.model

data class WalletView(
    val token: String,
    val account: String,
    val expiry: String,
    val paymentAccountReference: String,
    val requestorId: String,
    val status: String,
    val isActive: Boolean,
    val isSuspended: Boolean,
    val keyIndex: Int,
    val keyCheckValue: String,
    val tapsLeft: Int,
    val tapsPerKey: Int,
    val atc: Int,
)
