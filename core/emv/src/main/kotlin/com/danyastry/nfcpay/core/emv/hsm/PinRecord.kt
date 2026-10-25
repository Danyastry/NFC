package com.danyastry.nfcpay.core.emv.hsm

data class PinRecord(
    val pvki: Int,
    val pvv: String,
    val triesLeft: Int,
) {
    val isBlocked: Boolean
        get() = triesLeft == 0
}
