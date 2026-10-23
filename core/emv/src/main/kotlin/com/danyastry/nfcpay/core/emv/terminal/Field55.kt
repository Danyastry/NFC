package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.tlv.encode

object Field55 {

    val TAGS: List<Tag> = listOf(
        EmvTags.APPLICATION_CRYPTOGRAM,
        EmvTags.CRYPTOGRAM_INFORMATION_DATA,
        EmvTags.ISSUER_APPLICATION_DATA,
        EmvTags.UNPREDICTABLE_NUMBER,
        EmvTags.ATC,
        EmvTags.TVR,
        EmvTags.TRANSACTION_DATE,
        EmvTags.TRANSACTION_TYPE,
        EmvTags.AMOUNT_AUTHORISED,
        EmvTags.TRANSACTION_CURRENCY,
        EmvTags.AIP,
        EmvTags.TERMINAL_COUNTRY,
        EmvTags.AMOUNT_OTHER,
        EmvTags.TERMINAL_CAPABILITIES,
        EmvTags.TERMINAL_TYPE,
        EmvTags.DF_NAME,
        EmvTags.PAN_SEQUENCE_NUMBER,
    )

    fun build(values: Map<Tag, ByteArray>): ByteArray =
        TAGS.mapNotNull { tag -> values[tag]?.let { Tlv.primitive(tag, it) } }.encode()
}
