package com.danyastry.nfcpay.core.emv.pin

enum class PinBlockFormat(val control: Int, val label: String) {
    FORMAT_0(0x0, "ISO 9564 format 0 · PIN xor PAN"),
    FORMAT_1(0x1, "ISO 9564 format 1 · no PAN"),
    FORMAT_3(0x3, "ISO 9564 format 3 · random fill xor PAN"),
    FORMAT_4(0x4, "ISO 9564 format 4 · AES"),
}
