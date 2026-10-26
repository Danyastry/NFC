package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReferenceVectorsTest {

    private val issuerMasterKey = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    @Test
    fun `option A matches pyemv`() {
        assertThat(IccKeyDerivation.optionA(issuerMasterKey, "99012345678901234", 45).toHex()).isEqualTo("67F8292358083E5EA7AB7FDA58D53B6B")
        assertThat(IccKeyDerivation.optionA(issuerMasterKey, "12345678901234567", 1).toHex()).isEqualTo("73AD54688CEF2934B0979857E3C719F1")
    }

    @Test
    fun `common session key matches pyemv`() {
        assertThat(SessionKeyDerivation.commonSessionKey(issuerMasterKey, 0x001C).toHex()).isEqualTo("E9FB384AF807B940FEDCEA613461B0C4")
    }

    @Test
    fun `application cryptograms match pyemv`() {
        val sessionKey = "29B33180E567CE38EA4CBC9D753B0E61".hexToBytes()
        assertThat(RetailMac.compute(sessionKey, "0123456789ABCDEF0123456789ABCDEF".hexToBytes()).toHex()).isEqualTo("FA624250B008B59A")

        val data = "000000002000" + "000000000000" + "0124" + "0000008000" + "0124" + "110309" + "00" + "3804823E" + "5800" + "0001"
        assertThat(RetailMac.compute("AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBB".hexToBytes(), data.hexToBytes()).toHex()).isEqualTo("3B76CF10FECD8789")
    }

    @Test
    fun `Visa CVN 10 matches pyemv`() {
        val cardKey = IccKeyDerivation.optionA(issuerMasterKey, "1234567890123456", 0)
        assertThat(KeyCheckValue.of(cardKey)).startsWith("BAB0")

        val cdolData = "000000004000" + "000000000000" + "0124" + "8000048000" + "0124" + "191105" + "01" + "52BF4585"
        val arqc = CardCryptogram.compute(cardKey, cdolData.hexToBytes(), "1800".hexToBytes(), 0x001C, "03A06010".hexToBytes(), CryptogramVersion.CARD_KEY)

        assertThat(arqc.toHex()).isEqualTo("29CCA15AE665FA2E")
        assertThat(Arpc.method1(cardKey, arqc, "3030".hexToBytes()).toHex()).isEqualTo("28993816AFAE4AEB")
    }

    @Test
    fun `zero padding leaves aligned data alone`() {
        assertThat(RetailMac.padMethod1(ByteArray(8))).hasLength(8)
        assertThat(RetailMac.padMethod1(ByteArray(9))).hasLength(16)
    }

    @Test
    fun `ARPC method 1 matches pyemv`() {
        assertThat(Arpc.method1("29B33180E567CE38EA4CBC9D753B0E61".hexToBytes(), "FA624250B008B59A".hexToBytes(), "0000".hexToBytes()).toHex())
            .isEqualTo("45D4255EEF10C920")
        assertThat(Arpc.method1("AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBB".hexToBytes(), "1234567890ABCDEF".hexToBytes(), "0000".hexToBytes()).toHex())
            .isEqualTo("F5E6F44147E2F1B0")
    }
}
