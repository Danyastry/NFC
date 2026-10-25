package com.danyastry.nfcpay.core.emv.hsm

enum class PinCheck(val description: String) {
    VERIFIED("PIN verified"),
    WRONG_PIN("Incorrect PIN"),
    BAD_BLOCK("The PIN block did not decrypt to a valid PIN"),
    BAD_KSN("The key serial number is not usable"),
}
