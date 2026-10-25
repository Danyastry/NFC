package com.danyastry.nfcpay.core.emv.hsm

import com.danyastry.nfcpay.core.emv.dukpt.AesDukpt
import com.danyastry.nfcpay.core.emv.dukpt.DukptKeyUsage
import com.danyastry.nfcpay.core.emv.dukpt.EncryptedPin
import com.danyastry.nfcpay.core.emv.pin.IsoPinBlock
import com.danyastry.nfcpay.core.emv.pin.PinBlockException
import com.danyastry.nfcpay.core.emv.pin.VisaPvv

class PinHsm(
    private val baseDerivationKey: ByteArray,
    private val pinVerificationKey: ByteArray,
) {

    fun verify(encrypted: EncryptedPin, pan: String, pvki: Int, storedPvv: String): PinCheck {
        if (!encrypted.ksn.isUsable) return PinCheck.BAD_KSN
        val pinKey = AesDukpt.hostWorkingKey(baseDerivationKey, encrypted.ksn, DukptKeyUsage.PIN_ENCRYPTION)
        val pin = try {
            IsoPinBlock.decipherFormat4(pinKey, encrypted.block, pan)
        } catch (_: PinBlockException) {
            return PinCheck.BAD_BLOCK
        }
        return if (VisaPvv.verify(pinVerificationKey, pvki, pin, pan, storedPvv)) PinCheck.VERIFIED else PinCheck.WRONG_PIN
    }

    fun issuePvv(pin: String, pan: String, pvki: Int): String = VisaPvv.generate(pinVerificationKey, pvki, pin, pan)
}
