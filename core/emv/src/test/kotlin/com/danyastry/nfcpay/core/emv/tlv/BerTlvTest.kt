package com.danyastry.nfcpay.core.emv.tlv

import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BerTlvTest {

    private val ppse = (
        "6F23840E325041592E5359532E4444463031A511BF0C0E610C4F07A00000000310108701 01"
        ).replace(" ", "")

    @Test
    fun `parses nested templates`() {
        val tlvs = BerTlv.parse(ppse.hexToBytes())

        val fci = tlvs.single()
        assertThat(fci.tag).isEqualTo(EmvTags.FCI_TEMPLATE)
        assertThat(fci.children.map { it.tag }).containsExactly(EmvTags.DF_NAME, EmvTags.FCI_PROPRIETARY).inOrder()
        assertThat(tlvs.valueOf(EmvTags.DF_NAME)!!.toString(Charsets.US_ASCII)).isEqualTo("2PAY.SYS.DDF01")
        assertThat(tlvs.valueOf(EmvTags.AID)!!.toHex()).isEqualTo("A0000000031010")
        assertThat(tlvs.findTag(EmvTags.FCI_ISSUER_DISCRETIONARY)!!.children.single().tag).isEqualTo(EmvTags.APPLICATION_TEMPLATE)
    }

    @Test
    fun `encodes back to the same bytes`() {
        assertThat(BerTlv.parse(ppse.hexToBytes()).encode().toHex()).isEqualTo(ppse)
    }

    @Test
    fun `reads multi byte tags and long lengths`() {
        val long = ByteArray(200) { 0x41 }
        val bytes = "9F4E81C8".hexToBytes() + long + "9F3702CAFE".hexToBytes()

        val tlvs = BerTlv.parse(bytes)

        assertThat(tlvs.map { it.tag }).containsExactly(EmvTags.MERCHANT_NAME, EmvTags.UNPREDICTABLE_NUMBER).inOrder()
        assertThat(tlvs[0].value).hasLength(200)
        assertThat(tlvs[1].value.toHex()).isEqualTo("CAFE")
        assertThat(BerTlv.encodeLength(200).toHex()).isEqualTo("81C8")
        assertThat(BerTlv.encodeLength(300).toHex()).isEqualTo("82012C")
    }

    @Test
    fun `skips padding between objects`() {
        val tlvs = BerTlv.parse("00005A0812345678901234560000FF9F360200 2A".replace(" ", "").hexToBytes())

        assertThat(tlvs.map { it.tag }).containsExactly(EmvTags.PAN, EmvTags.ATC).inOrder()
        assertThat(tlvs[1].value.toHex()).isEqualTo("002A")
    }

    @Test
    fun `builds constructed objects from children`() {
        val tlv = Tlv.constructed(EmvTags.RESPONSE_FORMAT_2, Tlv.primitive(EmvTags.AIP, "1880".hexToBytes()))

        assertThat(tlv.encode().toHex()).isEqualTo("770482021880")
        assertThrows(IllegalArgumentException::class.java) { Tlv.constructed(EmvTags.PAN) }
    }

    @Test
    fun `keeps a constructed tag with a broken body as raw bytes`() {
        val tlv = BerTlv.parse("7003 5A05 12".replace(" ", "").hexToBytes()).single()

        assertThat(tlv.children).isEmpty()
        assertThat(tlv.value.toHex()).isEqualTo("5A0512")
    }

    @Test
    fun `reports truncated input with its offset`() {
        val error = assertThrows(TlvFormatException::class.java) { BerTlv.parse("5A0812345678".hexToBytes()) }
        assertThat(error.offset).isEqualTo(0)
        assertThrows(TlvFormatException::class.java) { BerTlv.parse("9F".hexToBytes()) }
        assertThrows(TlvFormatException::class.java) { BerTlv.parse("5A".hexToBytes()) }
        assertThrows(TlvFormatException::class.java) { BerTlv.parse("5A8501".hexToBytes()) }
    }
}
