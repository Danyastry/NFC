package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.dukpt.EncryptedPin
import java.time.LocalDate

class OnlineRequest(
    val card: CardSummary,
    val field55: ByteArray,
    val date: LocalDate,
    val pin: EncryptedPin? = null,
)
