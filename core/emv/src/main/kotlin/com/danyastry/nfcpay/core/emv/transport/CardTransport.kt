package com.danyastry.nfcpay.core.emv.transport

fun interface CardTransport {
    suspend fun transceive(command: ByteArray): ByteArray
}
