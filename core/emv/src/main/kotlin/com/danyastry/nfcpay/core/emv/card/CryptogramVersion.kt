package com.danyastry.nfcpay.core.emv.card

enum class CryptogramVersion(val code: Int, val label: String) {
    CARD_KEY(0x0A, "Visa CVN 10 - card master key"),
    SESSION_KEY(0x12, "CVN 18 style - EMV common session key"),
    LIMITED_USE_KEY(0x43, "CVN 43 style - limited-use key");

    companion object {
        fun of(code: Int): CryptogramVersion? = entries.firstOrNull { it.code == code }
    }
}
