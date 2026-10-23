package com.danyastry.nfcpay.core.nfc.hce

class ServedApdu(
    val source: ApduSource,
    val command: ByteArray,
    val response: ByteArray,
    val timeMillis: Long,
)
