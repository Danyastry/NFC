package com.danyastry.nfcpay.feature.issuer.model

data class KeyHierarchy(
    val issuerMasterKeyCheck: String,
    val cardKeyCheck: String,
    val cardKeyDerivation: String,
)
