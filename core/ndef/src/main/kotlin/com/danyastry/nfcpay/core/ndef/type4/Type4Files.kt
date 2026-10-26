package com.danyastry.nfcpay.core.ndef.type4

import com.danyastry.nfcpay.core.emv.util.hexToBytes

object Type4Files {
    val NDEF_APPLICATION: ByteArray = "D2760000850101".hexToBytes()
    const val CAPABILITY_CONTAINER = 0xE103
    const val DEFAULT_NDEF_FILE = 0xE104
    const val INS_SELECT = 0xA4
    const val INS_READ_BINARY = 0xB0
    const val INS_UPDATE_BINARY = 0xD6
    const val NLEN_SIZE = 2
}
