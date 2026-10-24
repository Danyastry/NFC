package com.danyastry.nfcpay.core.emv.card

enum class CardSession {
    IDLE,
    SELECTED,
    PROCESSING,
    AWAITING_ISSUER,
    COMPLETED,
}
