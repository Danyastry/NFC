package com.danyastry.nfcpay.core.emv.apdu

enum class Instruction(val cla: Int, val ins: Int, val label: String) {
    SELECT(0x00, 0xA4, "SELECT"),
    READ_RECORD(0x00, 0xB2, "READ RECORD"),
    GET_PROCESSING_OPTIONS(0x80, 0xA8, "GET PROCESSING OPTIONS"),
    GENERATE_AC(0x80, 0xAE, "GENERATE AC"),
    GET_DATA(0x80, 0xCA, "GET DATA");

    companion object {
        fun of(cla: Int, ins: Int): Instruction? = entries.firstOrNull { it.cla == cla && it.ins == ins }
    }
}
