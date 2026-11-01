package com.danyastry.nfcpay.core.emv.response

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResponseCodeTest {

    @Test
    fun `codes are unique two-character values`() {
        assertThat(ResponseCode.entries.map { it.code }).containsNoDuplicates()
        assertThat(ResponseCode.entries.map { it.code.length }.toSet()).containsExactly(2)
    }

    @Test
    fun `looks codes up by their wire value`() {
        assertThat(ResponseCode.of("00")).isEqualTo(ResponseCode.APPROVED)
        assertThat(ResponseCode.of("Z3")).isEqualTo(ResponseCode.UNABLE_TO_GO_ONLINE)
        assertThat(ResponseCode.of("99")).isNull()
        assertThat(ResponseCode.APPROVED.bytes).isEqualTo("00".toByteArray(Charsets.US_ASCII))
    }
}
