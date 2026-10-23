package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.crypto.RetailMac

object CardCryptogram {

    fun cvr(type: CryptogramType): ByteArray =
        byteArrayOf(0x03, (SECOND_AC_NOT_REQUESTED or (type.bits ushr 2)).toByte(), 0x00, 0x00)

    fun issuerApplicationData(type: CryptogramType): ByteArray =
        byteArrayOf(0x06, DERIVATION_KEY_INDEX, CRYPTOGRAM_VERSION) + cvr(type).copyOfRange(0, 4)

    fun compute(key: ByteArray, cdolData: ByteArray, aip: ByteArray, atc: ByteArray, type: CryptogramType): ByteArray =
        RetailMac.compute(key, cdolData + aip + atc + cvr(type))

    const val CRYPTOGRAM_VERSION: Byte = 0x0A
    const val DERIVATION_KEY_INDEX: Byte = 0x01
    private const val SECOND_AC_NOT_REQUESTED = 0x80
}
