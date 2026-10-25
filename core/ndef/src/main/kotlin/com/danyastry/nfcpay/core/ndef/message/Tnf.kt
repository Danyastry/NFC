package com.danyastry.nfcpay.core.ndef.message

enum class Tnf(val code: Int, val label: String) {
    EMPTY(0x00, "Empty"),
    WELL_KNOWN(0x01, "NFC Forum well-known type"),
    MEDIA(0x02, "Media type"),
    ABSOLUTE_URI(0x03, "Absolute URI"),
    EXTERNAL(0x04, "NFC Forum external type"),
    UNKNOWN(0x05, "Unknown"),
    UNCHANGED(0x06, "Unchanged"),
    RESERVED(0x07, "Reserved");

    companion object {
        fun of(code: Int): Tnf = entries.first { it.code == code and 0x07 }
    }
}
