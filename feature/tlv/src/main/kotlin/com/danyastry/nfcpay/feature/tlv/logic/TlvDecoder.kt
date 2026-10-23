package com.danyastry.nfcpay.feature.tlv.logic

import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException
import com.danyastry.nfcpay.core.emv.util.Hex
import com.danyastry.nfcpay.feature.tlv.model.TlvDecodeResult

object TlvDecoder {

    fun decode(text: String): TlvDecodeResult {
        if (text.isBlank()) return TlvDecodeResult.Empty
        val bytes = try {
            Hex.decode(text)
        } catch (error: IllegalArgumentException) {
            return TlvDecodeResult.Invalid(error.message.orEmpty())
        }
        return try {
            TlvDecodeResult.Decoded(BerTlv.parse(bytes), bytes.size)
        } catch (error: TlvFormatException) {
            TlvDecodeResult.Invalid(error.message.orEmpty())
        }
    }
}
