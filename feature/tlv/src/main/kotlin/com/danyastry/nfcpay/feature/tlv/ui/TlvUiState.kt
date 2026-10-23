package com.danyastry.nfcpay.feature.tlv.ui

import com.danyastry.nfcpay.feature.tlv.model.TlvDecodeResult

data class TlvUiState(
    val input: String = "",
    val result: TlvDecodeResult = TlvDecodeResult.Empty,
)
