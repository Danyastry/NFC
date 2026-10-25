package com.danyastry.nfcpay.core.nfc.wallet

import com.danyastry.nfcpay.core.emv.token.TokenRecord

data class WalletState(
    val token: TokenRecord,
    val keyIndex: Int,
    val keyCheckValue: String,
    val tapsLeft: Int,
    val tapsPerKey: Int,
    val atc: Int,
)
