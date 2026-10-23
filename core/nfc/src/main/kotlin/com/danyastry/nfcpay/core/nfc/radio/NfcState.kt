package com.danyastry.nfcpay.core.nfc.radio

data class NfcState(
    val hasNfc: Boolean,
    val hasCardEmulation: Boolean,
    val isEnabled: Boolean,
)
