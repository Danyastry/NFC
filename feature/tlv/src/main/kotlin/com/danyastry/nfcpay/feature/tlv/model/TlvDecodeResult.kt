package com.danyastry.nfcpay.feature.tlv.model

import com.danyastry.nfcpay.core.emv.tlv.Tlv

sealed interface TlvDecodeResult {
    data object Empty : TlvDecodeResult
    data class Decoded(val tlvs: List<Tlv>, val byteCount: Int) : TlvDecodeResult
    data class Invalid(val reason: String) : TlvDecodeResult
}
