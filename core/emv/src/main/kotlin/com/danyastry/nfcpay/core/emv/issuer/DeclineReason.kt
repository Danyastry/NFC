package com.danyastry.nfcpay.core.emv.issuer

enum class DeclineReason(val responseCode: ResponseCode, val description: String) {
    MISSING_DATA(ResponseCode.DO_NOT_HONOR, "Field 55 is missing data the cryptogram covers"),
    UNKNOWN_ACCOUNT(ResponseCode.INVALID_CARD_NUMBER, "No account for this card"),
    UNKNOWN_CRYPTOGRAM_VERSION(ResponseCode.DO_NOT_HONOR, "Unknown cryptogram version"),
    CRYPTOGRAM_MISMATCH(ResponseCode.SECURITY_VIOLATION, "ARQC does not match: wrong key or changed data"),
    REPLAYED_COUNTER(ResponseCode.SECURITY_VIOLATION, "Transaction counter already used: replayed cryptogram"),
    CARD_DECLINED(ResponseCode.DO_NOT_HONOR, "The card itself declined with an AAC"),
    EXPIRED_CARD(ResponseCode.EXPIRED_CARD, "Card expired"),
    INSUFFICIENT_FUNDS(ResponseCode.INSUFFICIENT_FUNDS, "Amount is above the available balance"),
}
