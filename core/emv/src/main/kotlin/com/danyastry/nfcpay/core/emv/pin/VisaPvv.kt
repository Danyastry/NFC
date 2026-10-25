package com.danyastry.nfcpay.core.emv.pin

import com.danyastry.nfcpay.core.emv.crypto.TripleDes
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import java.security.MessageDigest

object VisaPvv {

    const val LENGTH = 4

    fun generate(pvk: ByteArray, pvki: Int, pin: String, pan: String): String {
        require(pvki in 0..9) { "PVKI is one digit, got $pvki" }
        require(pin.length >= LENGTH && pin.all(Char::isDigit)) { "PIN must be at least $LENGTH digits" }
        require(pan.length >= 12 && pan.all(Char::isDigit)) { "PAN must be at least 12 digits" }
        val encrypted = TripleDes.encrypt(pvk, transformedSecurityParameter(pvki, pin, pan).hexToBytes()).toHex()
        val digits = encrypted.filter(Char::isDigit)
        val letters = encrypted.filter { it in 'A'..'F' }.map { '0' + (it - 'A') }.joinToString("")
        return (digits + letters).take(LENGTH)
    }

    fun verify(pvk: ByteArray, pvki: Int, pin: String, pan: String, storedPvv: String): Boolean =
        MessageDigest.isEqual(generate(pvk, pvki, pin, pan).toByteArray(), storedPvv.toByteArray())

    internal fun transformedSecurityParameter(pvki: Int, pin: String, pan: String): String =
        pan.dropLast(1).takeLast(11) + pvki + pin.take(LENGTH)
}
