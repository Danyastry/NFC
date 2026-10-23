package com.danyastry.nfcpay.core.emv.tlv

class TlvFormatException(message: String, val offset: Int) : IllegalArgumentException("$message at byte $offset")
