package com.danyastry.nfcpay.core.nfc.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.danyastry.nfcpay.core.nfc.lab.AppLab

class PaymentCardService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray =
        AppLab.lab.card.process(commandApdu, ApduSource.NFC)

    override fun onDeactivated(reason: Int) {
        AppLab.lab.card.deactivate()
    }
}
