package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.tlv.valueOf
import com.danyastry.nfcpay.core.emv.util.toHex
import java.time.YearMonth

object CardDataReader {

    fun pan(data: List<Tlv>): String? =
        data.valueOf(EmvTags.PAN)?.toHex()?.trimEnd('F')
            ?: track2(data)?.substringBefore('D')?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }

    fun expiry(data: List<Tlv>): YearMonth? {
        val digits = data.valueOf(EmvTags.EXPIRATION_DATE)?.toHex()?.take(4)
            ?: track2(data)?.substringAfter('D', "")?.take(4)
        return digits?.let(::yearMonth)
    }

    fun effective(data: List<Tlv>): YearMonth? = data.valueOf(EmvTags.EFFECTIVE_DATE)?.toHex()?.take(4)?.let(::yearMonth)

    fun panSequenceNumber(data: List<Tlv>): Int? = data.valueOf(EmvTags.PAN_SEQUENCE_NUMBER)?.toHex()?.toIntOrNull()

    fun text(data: List<Tlv>, tag: Tag): String? =
        data.valueOf(tag)?.toString(Charsets.US_ASCII)?.trim()?.takeIf { it.isNotEmpty() }

    private fun track2(data: List<Tlv>): String? = data.valueOf(EmvTags.TRACK_2_EQUIVALENT)?.toHex()

    private fun yearMonth(digits: String): YearMonth? {
        if (digits.length != 4 || !digits.all(Char::isDigit)) return null
        val month = digits.substring(2, 4).toInt()
        return if (month in 1..12) YearMonth.of(2000 + digits.substring(0, 2).toInt(), month) else null
    }
}
