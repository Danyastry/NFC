package com.danyastry.nfcpay.core.emv.dol

import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DolTest {

    private val pdol = "9F66049F02069F37045F2A02"

    @Test
    fun `parses tag and length pairs`() {
        val entries = Dol.parse(pdol.hexToBytes())

        assertThat(entries).containsExactly(
            DolEntry(EmvTags.TTQ, 4),
            DolEntry(EmvTags.AMOUNT_AUTHORISED, 6),
            DolEntry(EmvTags.UNPREDICTABLE_NUMBER, 4),
            DolEntry(EmvTags.TRANSACTION_CURRENCY, 2),
        ).inOrder()
        assertThat(Dol.totalLength(entries)).isEqualTo(16)
        assertThat(Dol.encode(entries).toHex()).isEqualTo(pdol)
    }

    @Test
    fun `fills missing values with zeros`() {
        val data = Dol.build(Dol.parse(pdol.hexToBytes())) { tag -> if (tag == EmvTags.UNPREDICTABLE_NUMBER) "CAFEBABE".hexToBytes() else null }

        assertThat(data.toHex()).isEqualTo("00000000" + "000000000000" + "CAFEBABE" + "0000")
    }

    @Test
    fun `pads and truncates numeric values on the left and others on the right`() {
        assertThat(Dol.fit(DolEntry(EmvTags.AMOUNT_AUTHORISED, 6), "1234".hexToBytes()).toHex()).isEqualTo("000000001234")
        assertThat(Dol.fit(DolEntry(EmvTags.AMOUNT_AUTHORISED, 2), "000000001234".hexToBytes()).toHex()).isEqualTo("1234")
        assertThat(Dol.fit(DolEntry(EmvTags.TTQ, 4), "26".hexToBytes()).toHex()).isEqualTo("26000000")
        assertThat(Dol.fit(DolEntry(EmvTags.MERCHANT_NAME, 2), "414243".hexToBytes()).toHex()).isEqualTo("4142")
    }

    @Test
    fun `splits data back into values`() {
        val entries = Dol.parse("9F02069F370 4".replace(" ", "").hexToBytes())
        val values = Dol.split(entries, "000000001234CAFEBABE".hexToBytes())

        assertThat(values.getValue(EmvTags.AMOUNT_AUTHORISED).toHex()).isEqualTo("000000001234")
        assertThat(values.getValue(EmvTags.UNPREDICTABLE_NUMBER).toHex()).isEqualTo("CAFEBABE")
        assertThrows(IllegalArgumentException::class.java) { Dol.split(entries, ByteArray(3)) }
    }

    @Test
    fun `rejects a zero tag and a missing length`() {
        assertThrows(TlvFormatException::class.java) { Dol.parse("00".hexToBytes()) }
        assertThrows(TlvFormatException::class.java) { Dol.parse("9F66".hexToBytes()) }
    }
}
