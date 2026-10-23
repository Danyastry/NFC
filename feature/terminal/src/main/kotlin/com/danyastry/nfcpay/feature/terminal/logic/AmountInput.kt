package com.danyastry.nfcpay.feature.terminal.logic

import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest

object AmountInput {

    private val PATTERN = Regex("""(\d{1,10})(?:[.,](\d{0,2}))?""")

    fun parse(text: String): Long? {
        val match = PATTERN.matchEntire(text.trim()) ?: return null
        val major = match.groupValues[1].toLong()
        val minor = match.groupValues[2].padEnd(2, '0').toLong()
        return (major * 100 + minor).takeIf { it in 1..TransactionRequest.MAX_AMOUNT }
    }

    fun format(minor: Long): String = "%d.%02d".format(minor / 100, minor % 100)
}
