package com.danyastry.nfcpay.core.emv.mask

import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SensitiveDataTest {

    @Test
    fun `keeps the BIN and the last four digits`() {
        assertThat(SensitiveData.maskPan("4111111111111111")).isEqualTo("411111******1111")
        assertThat(SensitiveData.maskPan("1234567890")).isEqualTo("**********")
        assertThat(SensitiveData.groupPan("411111******1111")).isEqualTo("4111 11** **** 1111")
    }

    @Test
    fun `masks card data only when asked`() {
        val pan = Tlv.primitive(EmvTags.PAN, "4111111111111111".hexToBytes())
        val track2 = Tlv.primitive(EmvTags.TRACK_2_EQUIVALENT, "4111111111111111D2512201".hexToBytes())
        val name = Tlv.primitive(EmvTags.CARDHOLDER_NAME, "AB".toByteArray())
        val atc = Tlv.primitive(EmvTags.ATC, "0001".hexToBytes())

        assertThat(SensitiveData.displayValue(pan, mask = true)).isEqualTo("411111******1111")
        assertThat(SensitiveData.displayValue(track2, mask = true)).isEqualTo("411111******1111D*******")
        assertThat(SensitiveData.displayValue(name, mask = true)).isEqualTo("****")
        assertThat(SensitiveData.displayValue(atc, mask = true)).isEqualTo("0001")
        assertThat(SensitiveData.displayValue(pan, mask = false)).isEqualTo("4111111111111111")
    }

    @Test
    fun `finds card data inside templates`() {
        val record = Tlv.constructed(EmvTags.RECORD_TEMPLATE, Tlv.primitive(EmvTags.PAN, "4111111111111111".hexToBytes()))

        assertThat(SensitiveData.containsSensitive(listOf(record))).isTrue()
        assertThat(SensitiveData.containsSensitive(listOf(Tlv.primitive(EmvTags.ATC, ByteArray(2))))).isFalse()
    }
}
