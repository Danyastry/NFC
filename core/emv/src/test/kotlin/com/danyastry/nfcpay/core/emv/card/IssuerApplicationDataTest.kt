package com.danyastry.nfcpay.core.emv.card

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class IssuerApplicationDataTest {

    @Test
    fun `encodes length, key index, version and CVR in the Visa layout`() {
        val iad = IssuerApplicationData(derivationKeyIndex = 1, version = CryptogramVersion.CARD_KEY, cvr = "03A00000".hexToBytes())

        assertThat(iad.encode().toHex()).isEqualTo("06010A03A00000")
    }

    @Test
    fun `parses what it encodes`() {
        val parsed = IssuerApplicationData.parse("06054303A00000".hexToBytes())!!

        assertThat(parsed.derivationKeyIndex).isEqualTo(5)
        assertThat(parsed.version).isEqualTo(CryptogramVersion.LIMITED_USE_KEY)
        assertThat(parsed.cvr.toHex()).isEqualTo("03A00000")
        assertThat(parsed.encode().toHex()).isEqualTo("06054303A00000")
    }

    @Test
    fun `short data and unknown versions are not IAD`() {
        assertThat(IssuerApplicationData.parse("06010A03A000".hexToBytes())).isNull()
        assertThat(IssuerApplicationData.parse("06017703A00000".hexToBytes())).isNull()
    }

    @Test
    fun `matches what the card builds`() {
        val built = CardCryptogram.issuerApplicationData(CryptogramType.ARQC, CryptogramVersion.SESSION_KEY)

        assertThat(IssuerApplicationData.parse(built)!!.encode()).isEqualTo(built)
    }

    @Test
    fun `the CVR cannot be changed from outside`() {
        val iad = IssuerApplicationData(1, CryptogramVersion.CARD_KEY, "03A00000".hexToBytes())

        iad.cvr[0] = 0x7F

        assertThat(iad.cvr.toHex()).isEqualTo("03A00000")
        assertThrows(IllegalArgumentException::class.java) { IssuerApplicationData(1, CryptogramVersion.CARD_KEY, ByteArray(3)) }
    }
}
