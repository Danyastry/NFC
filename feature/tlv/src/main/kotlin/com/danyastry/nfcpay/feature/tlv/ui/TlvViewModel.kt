package com.danyastry.nfcpay.feature.tlv.ui

import androidx.lifecycle.ViewModel
import com.danyastry.nfcpay.feature.tlv.logic.TlvDecoder
import com.danyastry.nfcpay.feature.tlv.model.TlvSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TlvViewModel : ViewModel() {

    private val _state = MutableStateFlow(TlvUiState())
    val state: StateFlow<TlvUiState> = _state.asStateFlow()

    fun onInputChange(text: String) {
        _state.value = TlvUiState(input = text, result = TlvDecoder.decode(text))
    }

    fun onSample(sample: TlvSample) = onInputChange(sample.hex())

    fun onClear() = onInputChange("")
}
