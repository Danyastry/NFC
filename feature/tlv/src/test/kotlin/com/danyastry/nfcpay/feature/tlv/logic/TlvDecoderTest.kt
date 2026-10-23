package com.danyastry.nfcpay.feature.tlv.logic

import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.feature.tlv.model.TlvDecodeResult
import com.danyastry.nfcpay.feature.tlv.model.TlvSample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TlvDecoderTest {

    @Test
    fun `blank input is empty`() {
        assertThat(TlvDecoder.decode("  \n")).isEqualTo(TlvDecodeResult.Empty)
    }

    @Test
    fun `decodes spaced hex`() {
        val result = TlvDecoder.decode("9F36 02 00 2A\n9F27 01 80") as TlvDecodeResult.Decoded

        assertThat(result.byteCount).isEqualTo(9)
        assertThat(result.tlvs.map { it.tag }).containsExactly(EmvTags.ATC, EmvTags.CRYPTOGRAM_INFORMATION_DATA).inOrder()
    }

    @Test
    fun `explains bad hex and bad TLV`() {
        assertThat(TlvDecoder.decode("9F3")).isInstanceOf(TlvDecodeResult.Invalid::class.java)
        assertThat((TlvDecoder.decode("5A08 1234") as TlvDecodeResult.Invalid).reason).contains("at byte 0")
    }

    @Test
    fun `every sample decodes`() {
        TlvSample.entries.forEach { sample ->
            assertThat(TlvDecoder.decode(sample.hex())).isInstanceOf(TlvDecodeResult.Decoded::class.java)
        }
    }
}
