package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.CryptogramType

data class Cryptogram(
    val type: CryptogramType,
    val value: String,
    val atc: Int,
    val issuerApplicationData: String,
    val fromProcessingOptions: Boolean,
)
