package com.danyastry.nfcpay.feature.tlv.ui

import com.danyastry.nfcpay.feature.tlv.model.TlvDecodeResult
import com.danyastry.nfcpay.feature.tlv.model.TlvSample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TlvViewModelTest {

    @Test
    fun `decodes as the user types and loads samples`() {
        val viewModel = TlvViewModel()

        viewModel.onInputChange("9F3602002A")
        assertThat(viewModel.state.value.result).isInstanceOf(TlvDecodeResult.Decoded::class.java)

        viewModel.onSample(TlvSample.PPSE)
        assertThat(viewModel.state.value.input).startsWith("6F")

        viewModel.onClear()
        assertThat(viewModel.state.value).isEqualTo(TlvUiState())
    }
}
