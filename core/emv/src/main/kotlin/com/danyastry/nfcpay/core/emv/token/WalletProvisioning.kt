package com.danyastry.nfcpay.core.emv.token

data class WalletProvisioning(
    val token: TokenRecord,
    val firstKey: LimitedUseKey,
)
