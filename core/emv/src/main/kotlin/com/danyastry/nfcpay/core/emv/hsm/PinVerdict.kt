package com.danyastry.nfcpay.core.emv.hsm

data class PinVerdict(
    val check: PinCheck?,
    val triesLeft: Int,
    val blocked: Boolean,
) {
    val isVerified: Boolean
        get() = check == PinCheck.VERIFIED
}
