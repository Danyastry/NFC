package com.danyastry.nfcpay.core.emv.hsm

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.dukpt.AesDukpt
import com.danyastry.nfcpay.core.emv.dukpt.DukptTerminal
import com.danyastry.nfcpay.core.emv.util.hexToBytes

object DemoHsm {

    const val CARD_PIN = "1234"

    val baseDerivationKey: ByteArray = "FEDCBA9876543210F1F1F1F1F1F1F1F1".hexToBytes()
    val pinVerificationKey: ByteArray = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
    val initialKeyId: ByteArray = "1234567890123456".hexToBytes()

    fun pinService(): PinService = PinService(PinHsm(baseDerivationKey, pinVerificationKey)).apply {
        enroll(DemoCards.lab.pan, CARD_PIN)
    }

    fun pinPadDevice(): DukptTerminal = DukptTerminal(AesDukpt.initialKey(baseDerivationKey, initialKeyId), initialKeyId)
}
