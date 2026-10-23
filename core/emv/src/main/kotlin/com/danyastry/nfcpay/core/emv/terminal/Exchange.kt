package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu

data class Exchange(
    val label: String,
    val command: CommandApdu,
    val response: ResponseApdu,
)
