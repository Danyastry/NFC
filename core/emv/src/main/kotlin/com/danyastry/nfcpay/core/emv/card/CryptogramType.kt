package com.danyastry.nfcpay.core.emv.card

enum class CryptogramType(val bits: Int, val label: String) {
    AAC(0x00, "AAC"),
    TC(0x40, "TC"),
    ARQC(0x80, "ARQC");

    companion object {
        const val MASK = 0xC0

        fun fromBits(value: Int): CryptogramType? = entries.firstOrNull { it.bits == value and MASK }
    }
}
