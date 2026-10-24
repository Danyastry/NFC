package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.dol.DolEntry
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.util.Bcd
import com.danyastry.nfcpay.core.emv.util.hexToBytes

class CardFiles(private val profile: CardProfile) {

    val aip: ByteArray = AIP

    val afl: ByteArray = byteArrayOf((SFI shl 3).toByte(), 0x01, RECORD_COUNT.toByte(), 0x00)

    val pdol: List<DolEntry> = listOf(
        DolEntry(EmvTags.TTQ, 4),
        DolEntry(EmvTags.AMOUNT_AUTHORISED, 6),
        DolEntry(EmvTags.UNPREDICTABLE_NUMBER, 4),
        DolEntry(EmvTags.TRANSACTION_CURRENCY, 2),
    )

    val cdol1: List<DolEntry> = CDOL1

    val cdol2: List<DolEntry> = CDOL2

    fun ppse(): ByteArray = Tlv.constructed(
        EmvTags.FCI_TEMPLATE,
        Tlv.primitive(EmvTags.DF_NAME, EmvCommands.PPSE),
        Tlv.constructed(
            EmvTags.FCI_PROPRIETARY,
            Tlv.constructed(
                EmvTags.FCI_ISSUER_DISCRETIONARY,
                Tlv.constructed(
                    EmvTags.APPLICATION_TEMPLATE,
                    Tlv.primitive(EmvTags.AID, profile.aid),
                    Tlv.primitive(EmvTags.APPLICATION_LABEL, profile.label.ascii()),
                    Tlv.primitive(EmvTags.APPLICATION_PRIORITY, byteArrayOf(0x01)),
                ),
            ),
        ),
    ).encode()

    fun applicationFci(): ByteArray = Tlv.constructed(
        EmvTags.FCI_TEMPLATE,
        Tlv.primitive(EmvTags.DF_NAME, profile.aid),
        Tlv.constructed(
            EmvTags.FCI_PROPRIETARY,
            Tlv.primitive(EmvTags.APPLICATION_LABEL, profile.label.ascii()),
            Tlv.primitive(EmvTags.APPLICATION_PRIORITY, byteArrayOf(0x01)),
            Tlv.primitive(EmvTags.PDOL, Dol.encode(pdol)),
            Tlv.primitive(EmvTags.LANGUAGE_PREFERENCE, "en".ascii()),
        ),
    ).encode()

    fun record(sfi: Int, number: Int): ByteArray? = when {
        sfi != SFI -> null
        number == 1 -> Tlv.constructed(
            EmvTags.RECORD_TEMPLATE,
            Tlv.primitive(EmvTags.TRACK_2_EQUIVALENT, track2()),
            Tlv.primitive(EmvTags.CARDHOLDER_NAME, profile.cardholderName.ascii()),
        ).encode()
        number == 2 -> Tlv.constructed(
            EmvTags.RECORD_TEMPLATE,
            Tlv.primitive(EmvTags.PAN, compressedNumeric(profile.pan)),
            Tlv.primitive(EmvTags.EXPIRATION_DATE, Bcd.encodeDigits(expiryDigits(), 3)),
            Tlv.primitive(EmvTags.EFFECTIVE_DATE, Bcd.encodeDigits(effectiveDigits(), 3)),
            Tlv.primitive(EmvTags.ISSUER_COUNTRY, Bcd.encode(profile.issuerCountryCode.toLong(), 2)),
            Tlv.primitive(EmvTags.PAN_SEQUENCE_NUMBER, Bcd.encode(profile.panSequenceNumber.toLong(), 1)),
            Tlv.primitive(EmvTags.APPLICATION_CURRENCY, Bcd.encode(profile.currencyCode.toLong(), 2)),
            Tlv.primitive(EmvTags.CDOL1, Dol.encode(cdol1)),
            Tlv.primitive(EmvTags.CDOL2, Dol.encode(cdol2)),
            Tlv.primitive(EmvTags.CVM_LIST, CVM_LIST),
        ).encode()
        else -> null
    }

    private fun track2(): ByteArray {
        val expiry = expiryDigits().take(4)
        val digits = "${profile.pan}D$expiry$SERVICE_CODE$TRACK_2_DISCRETIONARY"
        return (if (digits.length % 2 == 0) digits else digits + "F").hexToBytes()
    }

    private fun expiryDigits(): String = "%02d%02d%02d".format(
        profile.expiry.year % 100,
        profile.expiry.monthValue,
        profile.expiry.lengthOfMonth(),
    )

    private fun effectiveDigits(): String = "%02d%02d01".format(profile.effective.year % 100, profile.effective.monthValue)

    private fun compressedNumeric(digits: String): ByteArray =
        (if (digits.length % 2 == 0) digits else digits + "F").hexToBytes()

    private fun String.ascii(): ByteArray = toByteArray(Charsets.US_ASCII)

    companion object {
        const val SFI = 1
        const val RECORD_COUNT = 2
        const val SERVICE_CODE = "201"
        const val TRACK_2_DISCRETIONARY = "00000000000"

        val AIP: ByteArray = "1C80".hexToBytes()

        val CDOL1: List<DolEntry> = listOf(
            DolEntry(EmvTags.AMOUNT_AUTHORISED, 6),
            DolEntry(EmvTags.AMOUNT_OTHER, 6),
            DolEntry(EmvTags.TERMINAL_COUNTRY, 2),
            DolEntry(EmvTags.TVR, 5),
            DolEntry(EmvTags.TRANSACTION_CURRENCY, 2),
            DolEntry(EmvTags.TRANSACTION_DATE, 3),
            DolEntry(EmvTags.TRANSACTION_TYPE, 1),
            DolEntry(EmvTags.UNPREDICTABLE_NUMBER, 4),
        )

        val CDOL2: List<DolEntry> = listOf(
            DolEntry(EmvTags.AUTHORISATION_RESPONSE_CODE, 2),
            DolEntry(EmvTags.ISSUER_AUTHENTICATION_DATA, 10),
            DolEntry(EmvTags.TVR, 5),
            DolEntry(EmvTags.UNPREDICTABLE_NUMBER, 4),
        )

        val CVM_LIST: ByteArray = "00000000000000004203".hexToBytes() + "1F03".hexToBytes()
    }
}
