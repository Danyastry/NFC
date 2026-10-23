package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.terminal.PaymentScheme
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import java.time.YearMonth

object DemoCards {

    val lab = CardProfile(
        aid = PaymentScheme.LAB.rid.hexToBytes(),
        label = "NFC PAY LAB",
        pan = "9999001234567891",
        expiry = YearMonth.of(2031, 12),
        effective = YearMonth.of(2026, 1),
        cardholderName = "LAB/NFC PAY",
        panSequenceNumber = 1,
        issuerCountryCode = 840,
        currencyCode = 840,
        masterKey = "0123456789ABCDEFFEDCBA9876543210".hexToBytes(),
    )
}
