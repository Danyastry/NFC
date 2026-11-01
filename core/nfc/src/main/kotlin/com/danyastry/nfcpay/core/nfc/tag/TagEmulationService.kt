package com.danyastry.nfcpay.core.nfc.tag

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.danyastry.nfcpay.core.nfc.lab.AppLab

class TagEmulationService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray = AppLab.lab.tag.process(commandApdu)

    override fun onDeactivated(reason: Int) {
        AppLab.lab.tag.deactivate()
    }
}
