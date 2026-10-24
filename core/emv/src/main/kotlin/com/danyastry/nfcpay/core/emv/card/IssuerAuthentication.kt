package com.danyastry.nfcpay.core.emv.card

enum class IssuerAuthentication(val cvrBits: Int) {
    NOT_PERFORMED(0x00),
    PASSED(0x40),
    FAILED(0xC0),
}
