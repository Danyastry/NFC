package com.danyastry.nfcpay.feature.card.model

import com.danyastry.nfcpay.core.nfc.hce.ApduSource

data class ServedEntry(
    val label: String,
    val source: ApduSource,
    val command: String,
    val response: String,
    val sw: Int?,
    val timeMillis: Long,
)
