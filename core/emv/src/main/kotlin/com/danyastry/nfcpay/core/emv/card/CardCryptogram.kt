package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.crypto.RetailMac
import com.danyastry.nfcpay.core.emv.crypto.SessionKeyDerivation
import com.danyastry.nfcpay.core.emv.util.toBytes

object CardCryptogram {

    const val DERIVATION_KEY_INDEX: Byte = 0x01
    private const val SECOND_AC_NOT_REQUESTED = 0x80
    private const val FIRST_AC_ARQC = 0x20

    fun cvr(type: CryptogramType): ByteArray =
        byteArrayOf(0x03, (SECOND_AC_NOT_REQUESTED or (type.bits ushr 2)).toByte(), 0x00, 0x00)

    fun secondCvr(type: CryptogramType, issuerAuthentication: IssuerAuthentication): ByteArray =
        byteArrayOf(0x03, (type.bits or FIRST_AC_ARQC).toByte(), issuerAuthentication.cvrBits.toByte(), 0x00)

    fun issuerApplicationData(type: CryptogramType, version: CryptogramVersion): ByteArray =
        issuerApplicationData(version, cvr(type))

    fun issuerApplicationData(version: CryptogramVersion, cvr: ByteArray, derivationKeyIndex: Int = DERIVATION_KEY_INDEX.toInt()): ByteArray =
        IssuerApplicationData(derivationKeyIndex, version, cvr).encode()

    fun key(cardMasterKey: ByteArray, version: CryptogramVersion, atc: Int): ByteArray = when (version) {
        CryptogramVersion.CARD_KEY -> cardMasterKey
        CryptogramVersion.SESSION_KEY -> SessionKeyDerivation.commonSessionKey(cardMasterKey, atc)
        CryptogramVersion.LIMITED_USE_KEY -> cardMasterKey
    }

    fun compute(key: ByteArray, cdolData: ByteArray, aip: ByteArray, atc: Int, cvr: ByteArray, version: CryptogramVersion): ByteArray {
        val data = cdolData + aip + atc.toBytes(2) + cvr
        return when (version) {
            CryptogramVersion.CARD_KEY -> RetailMac.computeZeroPadded(key, data)
            CryptogramVersion.SESSION_KEY, CryptogramVersion.LIMITED_USE_KEY -> RetailMac.compute(key, data)
        }
    }
}
