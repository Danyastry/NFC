package com.danyastry.nfcpay.core.emv.transport

class ResponseChainException(val sw: Int) : Exception("The card kept answering %04X after the GET RESPONSE limit".format(sw))
