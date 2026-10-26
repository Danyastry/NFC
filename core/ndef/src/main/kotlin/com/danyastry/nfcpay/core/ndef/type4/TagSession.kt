package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.terminal.Exchange
import com.danyastry.nfcpay.core.ndef.message.NdefMessage

data class TagSession(
    val exchanges: List<Exchange>,
    val capabilities: CapabilityContainer?,
    val message: NdefMessage?,
    val error: TagError?,
) {
    val isSuccess: Boolean
        get() = error == null
}
