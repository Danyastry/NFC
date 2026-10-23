package com.danyastry.nfcpay.core.nfc.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class PaymentCardService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray =
        SharedCard.host.process(commandApdu, ApduSource.NFC)

    override fun onDeactivated(reason: Int) {
        SharedCard.host.deactivate()
    }
}
