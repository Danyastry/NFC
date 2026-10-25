package com.danyastry.nfcpay.feature.terminal.ui

import androidx.annotation.StringRes
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.terminal.TerminalError
import com.danyastry.nfcpay.feature.terminal.R
import com.danyastry.nfcpay.feature.terminal.model.CardSource

@get:StringRes
val CardSource.labelRes: Int
    get() = when (this) {
        CardSource.SIMULATED -> R.string.terminal_source_simulated
        CardSource.WALLET -> R.string.terminal_source_wallet
        CardSource.NFC -> R.string.terminal_source_nfc
    }

@get:StringRes
val CryptogramType.decisionRes: Int
    get() = when (this) {
        CryptogramType.ARQC -> R.string.terminal_decision_arqc
        CryptogramType.TC -> R.string.terminal_decision_tc
        CryptogramType.AAC -> R.string.terminal_decision_aac
    }

@get:StringRes
val TerminalError.messageRes: Int
    get() = when (this) {
        TerminalError.NO_SUPPORTED_APPLICATION -> R.string.terminal_error_no_application
        TerminalError.PROCESSING_OPTIONS_REFUSED -> R.string.terminal_error_gpo
        TerminalError.MISSING_AIP_OR_AFL -> R.string.terminal_error_aip_afl
        TerminalError.BAD_AFL -> R.string.terminal_error_bad_afl
        TerminalError.READ_RECORD_FAILED -> R.string.terminal_error_read_record
        TerminalError.MISSING_CDOL1 -> R.string.terminal_error_cdol1
        TerminalError.GENERATE_AC_FAILED -> R.string.terminal_error_generate_ac
        TerminalError.MISSING_CRYPTOGRAM -> R.string.terminal_error_cryptogram
        TerminalError.MISSING_PAN -> R.string.terminal_error_pan
        TerminalError.MALFORMED_RESPONSE -> R.string.terminal_error_malformed
        TerminalError.CARD_LOST -> R.string.terminal_error_card_lost
    }
