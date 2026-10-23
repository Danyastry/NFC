package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.util.hexToBytes

class TerminalConfig(
    val countryCode: Int = 840,
    val currencyCode: Int = 840,
    val terminalType: Int = 22,
    val capabilities: ByteArray = "004800".hexToBytes(),
    val additionalCapabilities: ByteArray = "6000F0A001".hexToBytes(),
    val transactionQualifiers: ByteArray = "26804000".hexToBytes(),
    val merchantName: String = "NFC PAY LAB TERMINAL",
    val floorLimit: Long = 0,
    val cvmLimit: Long = 5_000,
    val schemes: List<PaymentScheme> = PaymentScheme.entries,
)
