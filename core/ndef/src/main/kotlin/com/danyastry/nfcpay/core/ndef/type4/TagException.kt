package com.danyastry.nfcpay.core.ndef.type4

class TagException(val error: TagError) : Exception(error.name)
