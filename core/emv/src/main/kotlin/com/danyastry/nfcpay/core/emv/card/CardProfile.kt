package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.util.Luhn
import java.time.YearMonth

class CardProfile(
    val aid: ByteArray,
    val label: String,
    val pan: String,
    val expiry: YearMonth,
    val effective: YearMonth,
    val cardholderName: String,
    val panSequenceNumber: Int,
    val issuerCountryCode: Int,
    val currencyCode: Int,
    val masterKey: ByteArray,
) {
    init {
        require(Luhn.isValid(pan)) { "PAN fails the Luhn check" }
        require(pan.length in 12..19) { "PAN must be 12 to 19 digits, got ${pan.length}" }
        require(!effective.isAfter(expiry)) { "Card becomes effective after it expires" }
        require(panSequenceNumber in 0..99) { "PAN sequence number is two digits, got $panSequenceNumber" }
        require(masterKey.size == 16) { "The card key is a double-length DES key, got ${masterKey.size} bytes" }
    }
}
