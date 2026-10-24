package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.crypto.Arpc
import com.danyastry.nfcpay.core.emv.crypto.RetailMac
import com.danyastry.nfcpay.core.emv.crypto.SessionKeyDerivation
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.valueOf
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VirtualCardTest {

    private val card = VirtualCard(DemoCards.lab)
    private val pdolData = ByteArray(16)
    private val cdolData = "000000001234" + "000000000000" + "0840" + "8000008000" + "0840" + "261023" + "00" + "CAFEBABE"

    @Test
    fun `lists its application in the PPSE`() {
        val response = send(EmvCommands.select(EmvCommands.PPSE))

        assertThat(response.isSuccess).isTrue()
        val tlvs = BerTlv.parse(response.data)
        assertThat(tlvs.valueOf(EmvTags.AID)).isEqualTo(DemoCards.lab.aid)
        assertThat(tlvs.valueOf(EmvTags.APPLICATION_LABEL)!!.toString(Charsets.US_ASCII)).isEqualTo("NFC PAY LAB")
    }

    @Test
    fun `answers SELECT with its PDOL`() {
        val response = send(EmvCommands.select(DemoCards.lab.aid))

        assertThat(BerTlv.parse(response.data).valueOf(EmvTags.PDOL)!!.toHex()).isEqualTo("9F66049F02069F37045F2A02")
    }

    @Test
    fun `refuses unknown applications and instructions`() {
        assertThat(send(EmvCommands.select("A0000000031010".hexToBytes())).sw).isEqualTo(StatusWord.FILE_NOT_FOUND)
        assertThat(send(CommandApdu(0x00, 0x20, 0x00, 0x80)).sw).isEqualTo(StatusWord.INS_NOT_SUPPORTED)
        assertThat(send(CommandApdu(0x84, 0x20, 0x00, 0x80)).sw).isEqualTo(StatusWord.CLA_NOT_SUPPORTED)
        assertThat(ResponseApdu.parse(card.process(byteArrayOf(0x00))).sw).isEqualTo(StatusWord.WRONG_LENGTH)
    }

    @Test
    fun `needs a selected application before processing options`() {
        assertThat(send(EmvCommands.getProcessingOptions(pdolData)).sw).isEqualTo(StatusWord.CONDITIONS_NOT_SATISFIED)
    }

    @Test
    fun `checks the PDOL data length`() {
        send(EmvCommands.select(DemoCards.lab.aid))

        assertThat(send(EmvCommands.getProcessingOptions(ByteArray(3))).sw).isEqualTo(StatusWord.WRONG_LENGTH)
        assertThat(card.atc).isEqualTo(0)
    }

    @Test
    fun `starts a transaction and counts it`() {
        send(EmvCommands.select(DemoCards.lab.aid))
        val response = send(EmvCommands.getProcessingOptions(pdolData))

        val tlvs = BerTlv.parse(response.data)
        assertThat(tlvs.valueOf(EmvTags.AIP)!!.toHex()).isEqualTo("1C80")
        assertThat(tlvs.valueOf(EmvTags.AFL)!!.toHex()).isEqualTo("08010200")
        assertThat(card.atc).isEqualTo(1)
    }

    @Test
    fun `serves the records listed in the AFL`() {
        val first = BerTlv.parse(send(EmvCommands.readRecord(1, 1)).data)
        val second = BerTlv.parse(send(EmvCommands.readRecord(1, 2)).data)

        assertThat(first.valueOf(EmvTags.TRACK_2_EQUIVALENT)!!.toHex()).isEqualTo("9999001234567891D311220100000000000F")
        assertThat(second.valueOf(EmvTags.PAN)!!.toHex()).isEqualTo("9999001234567891")
        assertThat(second.valueOf(EmvTags.EXPIRATION_DATE)!!.toHex()).isEqualTo("311231")
        assertThat(second.valueOf(EmvTags.CDOL1)!!.toHex()).isEqualTo("9F02069F03069F1A0295055F2A029A039C019F3704")
        assertThat(send(EmvCommands.readRecord(1, 3)).sw).isEqualTo(StatusWord.RECORD_NOT_FOUND)
        assertThat(send(EmvCommands.readRecord(2, 1)).sw).isEqualTo(StatusWord.RECORD_NOT_FOUND)
    }

    @Test
    fun `returns an ARQC the issuer can recompute`() {
        startTransaction()

        val response = send(EmvCommands.generateAc(CryptogramType.ARQC, cdolData.hexToBytes()))

        val tlvs = BerTlv.parse(response.data)
        assertThat(tlvs.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("80")
        assertThat(tlvs.valueOf(EmvTags.ATC)!!.toHex()).isEqualTo("0001")
        assertThat(tlvs.valueOf(EmvTags.ISSUER_APPLICATION_DATA)!!.toHex()).isEqualTo("06011203A00000")
        val sessionKey = SessionKeyDerivation.commonSessionKey(DemoCards.lab.masterKey, atc = 1)
        val expected = RetailMac.compute(sessionKey, (cdolData + "1C80" + "0001" + "03A00000").hexToBytes())
        assertThat(tlvs.valueOf(EmvTags.APPLICATION_CRYPTOGRAM)).isEqualTo(expected)
    }

    @Test
    fun `never approves offline and honours a decline`() {
        startTransaction()
        val tc = BerTlv.parse(send(EmvCommands.generateAc(CryptogramType.TC, cdolData.hexToBytes())).data)
        assertThat(tc.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("80")

        startTransaction()
        val aac = BerTlv.parse(send(EmvCommands.generateAc(CryptogramType.AAC, cdolData.hexToBytes())).data)
        assertThat(aac.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("00")
        assertThat(aac.valueOf(EmvTags.ISSUER_APPLICATION_DATA)!!.toHex()).isEqualTo("06011203800000")
    }

    @Test
    fun `asks for the issuer answer through CDOL2`() {
        val record = BerTlv.parse(send(EmvCommands.readRecord(1, 2)).data)

        assertThat(record.valueOf(EmvTags.CDOL2)!!.toHex()).isEqualTo("8A0291 0A95059F3704".replace(" ", ""))
    }

    @Test
    fun `completes with a TC when the issuer approves and proves it`() {
        val arqc = firstArqc()

        val response = secondAc(CryptogramType.TC, "3030", issuerData(arqc, "3030"))

        assertThat(response.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("40")
        assertThat(response.valueOf(EmvTags.ISSUER_APPLICATION_DATA)!!.toHex()).isEqualTo("06011203604000")
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.PASSED)
        val cdol2Data = "3030" + issuerData(arqc, "3030") + "8000008000" + "CAFEBABE"
        val expected = RetailMac.compute(sessionKey(), (cdolData + cdol2Data + "1C80" + "0001" + "03604000").hexToBytes())
        assertThat(response.valueOf(EmvTags.APPLICATION_CRYPTOGRAM)).isEqualTo(expected)
    }

    @Test
    fun `declines when the ARPC is forged`() {
        firstArqc()

        val response = secondAc(CryptogramType.TC, "3030", "0102030405060708" + "3030")

        assertThat(response.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("00")
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.FAILED)
    }

    @Test
    fun `declines when the response code was changed after signing`() {
        val arqc = firstArqc()

        val response = secondAc(CryptogramType.TC, "3030", issuerData(arqc, "3531"))

        assertThat(response.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("00")
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.FAILED)
    }

    @Test
    fun `follows a genuine issuer decline`() {
        val arqc = firstArqc()

        val response = secondAc(CryptogramType.AAC, "3531", issuerData(arqc, "3531"))

        assertThat(response.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("00")
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.PASSED)
    }

    @Test
    fun `declines when the terminal could not go online`() {
        firstArqc()

        val response = secondAc(CryptogramType.AAC, "5A33", "00".repeat(10))

        assertThat(response.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)!!.toHex()).isEqualTo("00")
        assertThat(card.lastIssuerAuthentication).isEqualTo(IssuerAuthentication.NOT_PERFORMED)
    }

    @Test
    fun `allows exactly two cryptograms per transaction`() {
        val arqc = firstArqc()

        assertThat(send(EmvCommands.generateAc(CryptogramType.ARQC, ByteArray(21))).sw).isEqualTo(StatusWord.INCORRECT_P1_P2)
        assertThat(send(EmvCommands.generateAc(CryptogramType.TC, ByteArray(3))).sw).isEqualTo(StatusWord.WRONG_LENGTH)
        secondAc(CryptogramType.TC, "3030", issuerData(arqc, "3030"))
        assertThat(send(EmvCommands.generateAc(CryptogramType.TC, ByteArray(21))).sw).isEqualTo(StatusWord.CONDITIONS_NOT_SATISFIED)
    }

    @Test
    fun `an AAC ends the transaction at once`() {
        startTransaction()
        send(EmvCommands.generateAc(CryptogramType.AAC, cdolData.hexToBytes()))

        assertThat(send(EmvCommands.generateAc(CryptogramType.TC, ByteArray(21))).sw).isEqualTo(StatusWord.CONDITIONS_NOT_SATISFIED)
    }

    private fun firstArqc(): ByteArray {
        startTransaction()
        val response = BerTlv.parse(send(EmvCommands.generateAc(CryptogramType.ARQC, cdolData.hexToBytes())).data)
        return response.valueOf(EmvTags.APPLICATION_CRYPTOGRAM)!!
    }

    private fun secondAc(type: CryptogramType, responseCode: String, issuerData: String) =
        BerTlv.parse(send(EmvCommands.generateAc(type, (responseCode + issuerData + "8000008000" + "CAFEBABE").hexToBytes())).data)

    private fun issuerData(arqc: ByteArray, responseCode: String): String =
        Arpc.method1(sessionKey(), arqc, responseCode.hexToBytes()).toHex() + responseCode

    private fun sessionKey() = SessionKeyDerivation.commonSessionKey(DemoCards.lab.masterKey, atc = 1)

    @Test
    fun `checks the CDOL1 data length`() {
        startTransaction()

        assertThat(send(EmvCommands.generateAc(CryptogramType.ARQC, ByteArray(4))).sw).isEqualTo(StatusWord.WRONG_LENGTH)
    }

    @Test
    fun `reports the counter through GET DATA and can reset it`() {
        startTransaction()
        startTransaction()

        assertThat(send(EmvCommands.getData(EmvTags.ATC)).data.toHex()).isEqualTo("9F36020002")
        assertThat(send(EmvCommands.getData(EmvTags.PDOL)).sw).isEqualTo(StatusWord.REFERENCED_DATA_NOT_FOUND)
        card.resetCounter()
        assertThat(card.atc).isEqualTo(0)
    }

    @Test
    fun `holds the key derived from the issuer master key`() {
        assertThat(DemoCards.lab.masterKey.toHex()).isEqualTo("0116F2F17C70E09E2A4ACD43A8C29886")
    }

    @Test
    fun `can still sign with the card key directly`() {
        val profile = CardProfile(
            aid = DemoCards.lab.aid, label = "CVN 10", pan = DemoCards.lab.pan, expiry = DemoCards.lab.expiry,
            effective = DemoCards.lab.effective, cardholderName = "TEST", panSequenceNumber = 1, issuerCountryCode = 840,
            currencyCode = 840, masterKey = DemoCards.lab.masterKey, cryptogramVersion = CryptogramVersion.CARD_KEY,
        )
        val legacy = VirtualCard(profile)
        legacy.process(EmvCommands.select(profile.aid).encode())
        legacy.process(EmvCommands.getProcessingOptions(pdolData).encode())

        val tlvs = BerTlv.parse(ResponseApdu.parse(legacy.process(EmvCommands.generateAc(CryptogramType.ARQC, cdolData.hexToBytes()).encode())).data)

        assertThat(tlvs.valueOf(EmvTags.ISSUER_APPLICATION_DATA)!!.toHex()).isEqualTo("06010A03A00000")
        val expected = RetailMac.compute(DemoCards.lab.masterKey, (cdolData + "1C80" + "0001" + "03A00000").hexToBytes())
        assertThat(tlvs.valueOf(EmvTags.APPLICATION_CRYPTOGRAM)).isEqualTo(expected)
    }

    @Test
    fun `forgets the session when the field drops`() {
        send(EmvCommands.select(DemoCards.lab.aid))
        card.deactivate()

        assertThat(send(EmvCommands.getProcessingOptions(pdolData)).sw).isEqualTo(StatusWord.CONDITIONS_NOT_SATISFIED)
    }

    private fun startTransaction() {
        send(EmvCommands.select(DemoCards.lab.aid))
        assertThat(send(EmvCommands.getProcessingOptions(pdolData)).isSuccess).isTrue()
    }

    private fun send(command: CommandApdu): ResponseApdu = ResponseApdu.parse(card.process(command.encode()))
}
