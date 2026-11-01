package com.danyastry.nfcpay.core.emv.transport

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.CommandDescriber
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu

class ApduChannel(
    private val transport: CardTransport,
    private val describe: (CommandApdu) -> String = CommandDescriber::describe,
) {
    private val log = mutableListOf<Exchange>()

    val exchanges: List<Exchange>
        get() = log.toList()

    suspend fun exchange(command: CommandApdu, label: String = describe(command)): ResponseApdu {
        var response = send(label, command)
        if (response.sw ushr 8 == WRONG_LE) {
            val le = expectedLength(response.sw)
            response = send("$label - retry with Le %02X".format(le and 0xFF), CommandApdu(command.cla, command.ins, command.p1, command.p2, command.data, le))
        }
        var data = response.data
        var fetches = 0
        while (response.sw ushr 8 == MORE_DATA) {
            if (++fetches > MAX_GET_RESPONSE) throw ResponseChainException(response.sw)
            val getResponse = CommandApdu(0x00, GET_RESPONSE, 0x00, 0x00, le = expectedLength(response.sw))
            response = send(describe(getResponse), getResponse)
            data += response.data
        }
        return ResponseApdu(data, response.sw)
    }

    private suspend fun send(label: String, command: CommandApdu): ResponseApdu {
        val response = ResponseApdu.parse(transport.transceive(command.encode()))
        log += Exchange(label, command, response)
        return response
    }

    private fun expectedLength(sw: Int): Int = (sw and 0xFF).let { if (it == 0) CommandApdu.MAX_SHORT_LE else it }

    companion object {
        const val MORE_DATA = 0x61
        const val WRONG_LE = 0x6C
        const val GET_RESPONSE = 0xC0
        const val MAX_GET_RESPONSE = 16
    }
}
