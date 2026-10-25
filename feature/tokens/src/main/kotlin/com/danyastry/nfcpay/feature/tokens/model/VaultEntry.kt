package com.danyastry.nfcpay.feature.tokens.model

data class VaultEntry(
    val token: String,
    val domain: String,
    val status: String,
)
