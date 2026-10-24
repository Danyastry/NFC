package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.card.CryptogramType

sealed interface TransactionOutcome {

    data class Completed(
        val card: CardSummary,
        val cryptogram: Cryptogram,
        val tvr: Tvr,
        val cardholderVerificationRequired: Boolean,
        val field55: String,
        val online: OnlineResult? = null,
    ) : TransactionOutcome {
        val decision: CryptogramType
            get() = cryptogram.type
    }

    data class Failed(val error: TerminalError, val sw: Int? = null) : TransactionOutcome
}
