package com.danyastry.nfcpay.core.emv.issuer

enum class ResponseCode(val code: String, val description: String) {
    APPROVED("00", "Approved"),
    DO_NOT_HONOR("05", "Do not honor"),
    INVALID_CARD_NUMBER("14", "Invalid card number"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    INCORRECT_PIN("55", "Incorrect PIN"),
    EXPIRED_CARD("54", "Expired card"),
    RESTRICTED_CARD("62", "Restricted card"),
    SECURITY_VIOLATION("63", "Security violation"),
    PIN_TRIES_EXCEEDED("75", "PIN tries exceeded");

    val bytes: ByteArray
        get() = code.toByteArray(Charsets.US_ASCII)
}
