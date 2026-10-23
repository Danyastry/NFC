package com.danyastry.nfcpay.core.emv.terminal

import java.time.LocalDate

data class TransactionRequest(
    val amount: Long,
    val date: LocalDate,
    val amountOther: Long = 0,
    val type: Int = PURCHASE,
) {
    init {
        require(amount in 0..MAX_AMOUNT) { "Amount must be 0..$MAX_AMOUNT minor units, got $amount" }
        require(amountOther in 0..MAX_AMOUNT) { "Other amount must be 0..$MAX_AMOUNT minor units, got $amountOther" }
    }

    companion object {
        const val PURCHASE = 0x00
        const val MAX_AMOUNT = 999_999_999_999L
    }
}
