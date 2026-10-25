package com.danyastry.nfcpay.core.emv.hsm

import com.danyastry.nfcpay.core.emv.dukpt.EncryptedPin

class PinService(
    private val hsm: PinHsm,
    private val tryLimit: Int = DEFAULT_TRY_LIMIT,
) {
    private val records = mutableMapOf<String, PinRecord>()

    @Synchronized
    fun enroll(pan: String, pin: String, pvki: Int = DEFAULT_PVKI) {
        records[pan] = PinRecord(pvki, hsm.issuePvv(pin, pan, pvki), tryLimit)
    }

    @Synchronized
    fun record(pan: String): PinRecord? = records[pan]

    @Synchronized
    fun verify(pan: String, encrypted: EncryptedPin): PinVerdict {
        val record = records[pan] ?: return PinVerdict(check = null, triesLeft = 0, blocked = true)
        if (record.isBlocked) return PinVerdict(check = null, triesLeft = 0, blocked = true)
        val check = hsm.verify(encrypted, pan, record.pvki, record.pvv)
        val updated = when (check) {
            PinCheck.VERIFIED -> record.copy(triesLeft = tryLimit)
            PinCheck.WRONG_PIN -> record.copy(triesLeft = record.triesLeft - 1)
            PinCheck.BAD_BLOCK, PinCheck.BAD_KSN -> record
        }
        records[pan] = updated
        return PinVerdict(check, updated.triesLeft, updated.isBlocked)
    }

    @Synchronized
    fun unblock(pan: String) {
        records[pan]?.let { records[pan] = it.copy(triesLeft = tryLimit) }
    }

    companion object {
        const val DEFAULT_TRY_LIMIT = 3
        const val DEFAULT_PVKI = 1
    }
}
