package com.danyastry.nfcpay.core.nfc.tag

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class TagEmulationService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray = SharedTag.tag.process(commandApdu)

    override fun onDeactivated(reason: Int) {
        SharedTag.tag.deactivate()
    }
}
