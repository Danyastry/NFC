package com.danyastry.nfcpay.core.emv.card

fun interface ApduHandler {
    fun process(command: ByteArray): ByteArray
}
