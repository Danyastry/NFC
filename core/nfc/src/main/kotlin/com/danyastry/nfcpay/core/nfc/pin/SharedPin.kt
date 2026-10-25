package com.danyastry.nfcpay.core.nfc.pin

import com.danyastry.nfcpay.core.emv.dukpt.DukptTerminal
import com.danyastry.nfcpay.core.emv.hsm.DemoHsm
import com.danyastry.nfcpay.core.emv.hsm.PinService

object SharedPin {
    val service: PinService = DemoHsm.pinService()
    val device: DukptTerminal = DemoHsm.pinPadDevice()
}
