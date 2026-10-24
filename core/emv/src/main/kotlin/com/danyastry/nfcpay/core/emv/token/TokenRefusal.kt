package com.danyastry.nfcpay.core.emv.token

enum class TokenRefusal(val description: String) {
    UNKNOWN_TOKEN("The vault has no such token"),
    SUSPENDED("The token is suspended"),
    DELETED("The token was deleted"),
    WRONG_DOMAIN("The token is not allowed in this channel"),
    EXPIRED("The token has expired"),
}
