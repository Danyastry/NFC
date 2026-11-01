package com.danyastry.nfcpay.feature.card.logic

import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.nfc.hce.ApduSource
import com.danyastry.nfcpay.core.nfc.hce.ServedApdu
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ServedEntriesTest {

    @Test
    fun `maps a served command with its status`() {
        val entry = ServedEntries.map(ServedApdu(ApduSource.NFC, "00A40400".hexToBytes(), "6A82".hexToBytes(), timeMillis = 5))

        assertThat(entry.sw).isEqualTo(0x6A82)
        assertThat(entry.source).isEqualTo(ApduSource.NFC)
        assertThat(entry.response).isEqualTo("6A82")
    }

    @Test
    fun `shows the virtual card in full`() {
        val details = ServedEntries.details(DemoCards.lab)

        assertThat(details.pan).isEqualTo("9999 0012 3456 7891")
        assertThat(details.expiry).isEqualTo("12/31")
        assertThat(details.aid).isEqualTo("F04E464350415901")
    }
}
