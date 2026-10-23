package com.danyastry.nfcpay.core.emv.tags

import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmvDictionaryTest {

    @Test
    fun `names the tags the kernel uses`() {
        assertThat(EmvDictionary.nameOf(EmvTags.APPLICATION_CRYPTOGRAM)).isEqualTo("Application Cryptogram")
        assertThat(EmvDictionary.nameOf(EmvTags.AFL)).isEqualTo("Application File Locator")
        assertThat(EmvDictionary.nameOf(Tag(0xDF01))).isEqualTo("Unknown tag")
    }

    @Test
    fun `every named constant is in the dictionary`() {
        val constants = EmvTags::class.java.declaredFields
            .filter { it.type == Int::class.javaPrimitiveType && it.name != "INSTANCE" }
            .map { field -> field.isAccessible = true; Tag(field.getInt(EmvTags)) }

        assertThat(constants).isNotEmpty()
        constants.forEach { assertThat(EmvDictionary.info(it)).isNotNull() }
    }

    @Test
    fun `marks card data as sensitive`() {
        assertThat(EmvDictionary.isSensitive(EmvTags.PAN)).isTrue()
        assertThat(EmvDictionary.isSensitive(EmvTags.TRACK_2_EQUIVALENT)).isTrue()
        assertThat(EmvDictionary.isSensitive(EmvTags.CARDHOLDER_NAME)).isTrue()
        assertThat(EmvDictionary.isSensitive(EmvTags.APPLICATION_CRYPTOGRAM)).isFalse()
    }

    @Test
    fun `shows printable text tags as text`() {
        assertThat(TagText.of(Tlv.primitive(EmvTags.APPLICATION_LABEL, "VISA DEBIT".toByteArray()))).isEqualTo("VISA DEBIT")
        assertThat(TagText.of(Tlv.primitive(EmvTags.APPLICATION_LABEL, byteArrayOf(0x01)))).isNull()
        assertThat(TagText.of(Tlv.primitive(EmvTags.ATC, "AB".toByteArray()))).isNull()
    }
}
