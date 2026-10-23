package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TvrTest {

    @Test
    fun `sets each bit in its byte`() {
        val tvr = Tvr() + TvrBit.OFFLINE_DATA_AUTHENTICATION_NOT_PERFORMED + TvrBit.EXPIRED_APPLICATION + TvrBit.FLOOR_LIMIT_EXCEEDED

        assertThat(tvr.encode().toHex()).isEqualTo("8040008000")
    }

    @Test
    fun `decodes what it encodes`() {
        val tvr = Tvr.decode("8020048800".hexToBytes())

        assertThat(tvr.bits).containsExactly(
            TvrBit.OFFLINE_DATA_AUTHENTICATION_NOT_PERFORMED,
            TvrBit.APPLICATION_NOT_EFFECTIVE,
            TvrBit.ONLINE_PIN_ENTERED,
            TvrBit.FLOOR_LIMIT_EXCEEDED,
            TvrBit.MERCHANT_FORCED_ONLINE,
        )
        assertThat(tvr.encode().toHex()).isEqualTo("8020048800")
    }

    @Test
    fun `every bit is unique`() {
        assertThat(TvrBit.entries.map { it.byteIndex to it.mask }).containsNoDuplicates()
    }
}
