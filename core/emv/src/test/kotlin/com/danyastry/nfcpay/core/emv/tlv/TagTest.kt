package com.danyastry.nfcpay.core.emv.tlv

import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagTest {

    @Test
    fun `knows its bytes, class and form`() {
        assertThat(Tag(0x5A).bytes.toHex()).isEqualTo("5A")
        assertThat(Tag(0x9F26).bytes.toHex()).isEqualTo("9F26")
        assertThat(Tag(0x6F).isConstructed).isTrue()
        assertThat(Tag(0xBF0C).isConstructed).isTrue()
        assertThat(Tag(0x9F38).isConstructed).isFalse()
        assertThat(Tag(0x5F20).tagClass).isEqualTo(TagClass.APPLICATION)
        assertThat(Tag(0x9F02).tagClass).isEqualTo(TagClass.CONTEXT_SPECIFIC)
        assertThat(Tag.of("9F37")).isEqualTo(Tag(0x9F37))
    }
}
