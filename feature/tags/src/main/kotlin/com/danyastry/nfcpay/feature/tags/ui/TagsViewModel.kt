package com.danyastry.nfcpay.feature.tags.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.ndef.type4.TagSession
import com.danyastry.nfcpay.core.ndef.type4.Type4Reader
import com.danyastry.nfcpay.core.ndef.type4.Type4Tag
import com.danyastry.nfcpay.feature.tags.logic.MessageBuilder
import com.danyastry.nfcpay.feature.tags.logic.RecordViews
import com.danyastry.nfcpay.feature.tags.model.RecordKind
import com.danyastry.nfcpay.feature.tags.model.TagAction
import com.danyastry.nfcpay.feature.tags.model.TagSource
import com.danyastry.nfcpay.feature.tags.state.TagResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class TagsViewModel(
    private val tag: Type4Tag,
    private val reader: Type4Reader = Type4Reader(),
) : ViewModel() {

    private val _state = MutableStateFlow(TagsUiState())
    val state: StateFlow<TagsUiState> = _state.asStateFlow()

    fun onSourceChange(source: TagSource) {
        _state.update { if (it.isBusy) it else it.copy(source = source, waitingForTag = false) }
    }

    fun onKindChange(kind: RecordKind) {
        _state.update { it.copy(kind = kind) }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
    }

    fun onWrite() {
        val current = _state.value
        if (!current.canWrite) return
        val message = MessageBuilder.build(current.kind, current.input) ?: return
        run(TagAction.WRITE) { reader.write(LoopbackTransport(tag), message) }
    }

    fun onRead() {
        val current = _state.value
        if (current.isBusy) return
        when (current.source) {
            TagSource.VIRTUAL -> run(TagAction.READ) { reader.read(LoopbackTransport(tag)) }
            TagSource.NFC -> _state.update { it.copy(waitingForTag = true) }
        }
    }

    fun onCancel() {
        _state.update { it.copy(waitingForTag = false) }
    }

    fun onRealTag(read: suspend () -> ByteArray?) {
        if (!_state.value.waitingForTag) return
        _state.update { it.copy(waitingForTag = false, isBusy = true) }
        viewModelScope.launch {
            val result = try {
                val bytes = read()
                if (bytes == null) {
                    failure(NO_NDEF)
                } else {
                    RecordViews.parse(bytes).fold(
                        onSuccess = { TagResult(TagSource.NFC, TagAction.READ, RecordViews.of(it), bytes.toHex(), null, emptyList()) },
                        onFailure = { failure(it.message ?: NO_NDEF, bytes.toHex()) },
                    )
                }
            } catch (_: IOException) {
                failure(TAG_LOST)
            }
            _state.update { it.copy(isBusy = false, result = result) }
        }
    }

    private fun run(action: TagAction, block: suspend () -> TagSession) {
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val session = block()
            val result = TagResult(
                source = TagSource.VIRTUAL,
                action = action,
                records = session.message?.let(RecordViews::of).orEmpty(),
                messageHex = session.message?.encode()?.toHex(),
                error = session.error?.description,
                exchanges = session.exchanges,
            )
            _state.update { it.copy(isBusy = false, result = result) }
        }
    }

    private fun failure(error: String, hex: String? = null) = TagResult(TagSource.NFC, TagAction.READ, emptyList(), hex, error, emptyList())

    private companion object {
        const val NO_NDEF = "The tag holds no NDEF message"
        const val TAG_LOST = "The tag left the field"
    }
}
