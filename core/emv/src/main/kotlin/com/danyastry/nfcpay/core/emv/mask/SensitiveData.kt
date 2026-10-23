package com.danyastry.nfcpay.core.emv.mask

import com.danyastry.nfcpay.core.emv.tags.EmvDictionary
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.tlv.flatten
import com.danyastry.nfcpay.core.emv.util.toHex

object SensitiveData {

    private const val VISIBLE_START = 6
    private const val VISIBLE_END = 4

    fun maskPan(pan: String): String {
        if (pan.length <= VISIBLE_START + VISIBLE_END) return "*".repeat(pan.length)
        val hidden = pan.length - VISIBLE_START - VISIBLE_END
        return pan.take(VISIBLE_START) + "*".repeat(hidden) + pan.takeLast(VISIBLE_END)
    }

    fun groupPan(pan: String): String = pan.chunked(4).joinToString(" ")

    fun containsSensitive(tlvs: List<Tlv>): Boolean = tlvs.flatten().any { EmvDictionary.isSensitive(it.tag) }

    fun displayValue(tlv: Tlv, mask: Boolean): String {
        if (!mask || !EmvDictionary.isSensitive(tlv.tag)) return tlv.value.toHex()
        return when (tlv.tag) {
            EmvTags.PAN -> maskPan(tlv.value.toHex().trimEnd('F'))
            EmvTags.TRACK_2_EQUIVALENT, EmvTags.TRACK_2_DATA -> {
                val track = tlv.value.toHex()
                maskPan(track.substringBefore('D')) + "D" + "*".repeat(track.substringAfter('D', "").length)
            }
            else -> "*".repeat(tlv.value.size * 2)
        }
    }
}
