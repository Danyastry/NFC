package com.danyastry.nfcpay.core.emv.terminal

class KernelException(val error: TerminalError, val sw: Int? = null) : Exception(error.name)
