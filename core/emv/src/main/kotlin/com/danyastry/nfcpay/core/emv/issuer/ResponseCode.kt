package com.danyastry.nfcpay.core.emv.issuer

enum class ResponseCode(val code: String, val description: String) {
    APPROVED("00", "Approved"),
    DO_NOT_HONOR("05", "Do not honor"),
    INVALID_CARD_NUMBER("14", "Invalid card number"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    SECURITY_VIOLATION("63", "Security violation");

    val bytes: ByteArray
        get() = code.toByteArray(Charsets.US_ASCII)
}
