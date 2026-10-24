package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.crypto.RetailMac
import com.danyastry.nfcpay.core.emv.crypto.SessionKeyDerivation
import com.danyastry.nfcpay.core.emv.util.toBytes

object CardCryptogram {

    const val IAD_LENGTH = 7
    const val CVN_OFFSET = 2
    const val CVR_OFFSET = 3
    const val DERIVATION_KEY_INDEX: Byte = 0x01
    private const val SECOND_AC_NOT_REQUESTED = 0x80

    fun cvr(type: CryptogramType): ByteArray =
        byteArrayOf(0x03, (SECOND_AC_NOT_REQUESTED or (type.bits ushr 2)).toByte(), 0x00, 0x00)

    fun issuerApplicationData(type: CryptogramType, version: CryptogramVersion): ByteArray =
        byteArrayOf((IAD_LENGTH - 1).toByte(), DERIVATION_KEY_INDEX, version.code.toByte()) + cvr(type)

    fun key(cardMasterKey: ByteArray, version: CryptogramVersion, atc: Int): ByteArray = when (version) {
        CryptogramVersion.CARD_KEY -> cardMasterKey
        CryptogramVersion.SESSION_KEY -> SessionKeyDerivation.commonSessionKey(cardMasterKey, atc)
    }

    fun compute(key: ByteArray, cdolData: ByteArray, aip: ByteArray, atc: Int, cvr: ByteArray): ByteArray =
        RetailMac.compute(key, cdolData + aip + atc.toBytes(2) + cvr)
}
