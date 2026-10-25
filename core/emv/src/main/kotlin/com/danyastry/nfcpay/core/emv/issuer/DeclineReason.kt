package com.danyastry.nfcpay.core.emv.issuer

enum class DeclineReason(val responseCode: ResponseCode, val description: String) {
    MISSING_DATA(ResponseCode.DO_NOT_HONOR, "Field 55 is missing data the cryptogram covers"),
    UNKNOWN_ACCOUNT(ResponseCode.INVALID_CARD_NUMBER, "No account for this card"),
    UNKNOWN_CRYPTOGRAM_VERSION(ResponseCode.DO_NOT_HONOR, "Unknown cryptogram version"),
    TOKEN_REFUSED(ResponseCode.RESTRICTED_CARD, "The token vault refused the token"),
    WRONG_KEY_TYPE(ResponseCode.DO_NOT_HONOR, "Tokens sign with limited-use keys, cards with card keys"),
    KEY_RETIRED(ResponseCode.SECURITY_VIOLATION, "Limited-use key is not the active one"),
    KEY_EXHAUSTED(ResponseCode.SECURITY_VIOLATION, "Limited-use key has no taps left"),
    CRYPTOGRAM_MISMATCH(ResponseCode.SECURITY_VIOLATION, "ARQC does not match: wrong key or changed data"),
    REPLAYED_COUNTER(ResponseCode.SECURITY_VIOLATION, "Transaction counter already used: replayed cryptogram"),
    CARD_DECLINED(ResponseCode.DO_NOT_HONOR, "The card itself declined with an AAC"),
    EXPIRED_CARD(ResponseCode.EXPIRED_CARD, "Card expired"),
    INCORRECT_PIN(ResponseCode.INCORRECT_PIN, "The PIN does not match"),
    PIN_TRIES_EXCEEDED(ResponseCode.PIN_TRIES_EXCEEDED, "PIN is blocked after too many wrong tries"),
    UNREADABLE_PIN(ResponseCode.DO_NOT_HONOR, "The PIN block could not be decrypted"),
    INSUFFICIENT_FUNDS(ResponseCode.INSUFFICIENT_FUNDS, "Amount is above the available balance"),
}
