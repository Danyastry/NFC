package com.danyastry.nfcpay.feature.tlv.ui

import com.danyastry.nfcpay.feature.tlv.model.TlvSample

data class TlvActions(
    val onInputChange: (String) -> Unit,
    val onSample: (TlvSample) -> Unit,
    val onClear: () -> Unit,
)
