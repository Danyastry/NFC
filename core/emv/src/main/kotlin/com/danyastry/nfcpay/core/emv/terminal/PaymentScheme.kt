package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.util.toHex

enum class PaymentScheme(val label: String, val rid: String, val defaultAid: String) {
    LAB("NFC Pay Lab", "F04E464350415901", "F04E464350415901"),
    VISA("Visa", "A000000003", "A0000000031010"),
    MASTERCARD("Mastercard", "A000000004", "A0000000041010"),
    AMEX("American Express", "A000000025", "A00000002501"),
    DISCOVER("Discover", "A000000152", "A0000001523010"),
    JCB("JCB", "A000000065", "A0000000651010"),
    UNIONPAY("UnionPay", "A000000333", "A000000333010101"),
    MIR("Mir", "A000000658", "A0000006581010");

    companion object {
        fun of(aid: ByteArray): PaymentScheme? = aid.toHex().let { hex -> entries.firstOrNull { hex.startsWith(it.rid) } }
    }
}
