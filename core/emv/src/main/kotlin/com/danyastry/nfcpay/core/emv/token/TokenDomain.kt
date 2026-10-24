package com.danyastry.nfcpay.core.emv.token

enum class TokenDomain(val label: String) {
    CONTACTLESS("Contactless at a terminal"),
    ECOMMERCE("Online checkout"),
}
