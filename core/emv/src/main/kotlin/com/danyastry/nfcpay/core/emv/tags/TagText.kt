package com.danyastry.nfcpay.core.emv.tags

import com.danyastry.nfcpay.core.emv.tlv.Tlv

object TagText {

    fun of(tlv: Tlv): String? {
        if (EmvDictionary.info(tlv.tag)?.format != TagFormat.TEXT || tlv.value.isEmpty()) return null
        if (!tlv.value.all { it in PRINTABLE }) return null
        return tlv.value.toString(Charsets.US_ASCII)
    }

    private val PRINTABLE = 0x20.toByte()..0x7E.toByte()
}
