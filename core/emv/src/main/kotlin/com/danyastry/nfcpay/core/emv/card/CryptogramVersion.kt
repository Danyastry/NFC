package com.danyastry.nfcpay.core.emv.card

enum class CryptogramVersion(val code: Int, val label: String) {
    CARD_KEY(0x0A, "CVN 10 · card master key"),
    SESSION_KEY(0x12, "CVN 18 · EMV common session key");

    companion object {
        fun of(code: Int): CryptogramVersion? = entries.firstOrNull { it.code == code }
    }
}
