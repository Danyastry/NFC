package com.danyastry.nfcpay.core.emv.transport

import com.danyastry.nfcpay.core.emv.card.ApduHandler

class LoopbackTransport(private val card: ApduHandler) : CardTransport {
    override suspend fun transceive(command: ByteArray): ByteArray = card.process(command)
}
