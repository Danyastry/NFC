package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.crypto.IccKeyDerivation
import com.danyastry.nfcpay.core.emv.terminal.PaymentScheme
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import java.time.YearMonth

object DemoCards {

    val issuerMasterKey: ByteArray = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    private const val PAN = "9999001234567891"
    private const val PAN_SEQUENCE_NUMBER = 1

    val lab = CardProfile(
        aid = PaymentScheme.LAB.rid.hexToBytes(),
        label = "NFC PAY LAB",
        pan = PAN,
        expiry = YearMonth.of(2031, 12),
        effective = YearMonth.of(2026, 1),
        cardholderName = "LAB/NFC PAY",
        panSequenceNumber = PAN_SEQUENCE_NUMBER,
        issuerCountryCode = 840,
        currencyCode = 840,
        masterKey = IccKeyDerivation.optionA(issuerMasterKey, PAN, PAN_SEQUENCE_NUMBER),
    )
}
