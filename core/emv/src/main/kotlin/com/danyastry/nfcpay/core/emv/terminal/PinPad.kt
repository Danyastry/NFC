package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.dukpt.EncryptedPin

fun interface PinPad {
    suspend fun capture(pan: String, amount: Long): EncryptedPin?
}
