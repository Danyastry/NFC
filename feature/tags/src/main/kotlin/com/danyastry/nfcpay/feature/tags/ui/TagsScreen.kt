package com.danyastry.nfcpay.feature.tags.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.danyastry.nfcpay.core.nfc.radio.NfcAvailability
import com.danyastry.nfcpay.core.nfc.reader.TagReaderEffect
import com.danyastry.nfcpay.core.nfc.tag.NdefTagReader
import com.danyastry.nfcpay.core.nfc.tag.SharedTag
import com.danyastry.nfcpay.core.nfc.ui.ExchangeCard
import com.danyastry.nfcpay.core.nfc.ui.NfcStatusBanner
import com.danyastry.nfcpay.core.nfc.ui.rememberNfcAvailability
import com.danyastry.nfcpay.core.ui.components.SegmentedSelector
import com.danyastry.nfcpay.feature.tags.R
import com.danyastry.nfcpay.feature.tags.model.RecordKind
import com.danyastry.nfcpay.feature.tags.model.RecordView
import com.danyastry.nfcpay.feature.tags.model.TagAction
import com.danyastry.nfcpay.feature.tags.model.TagSource
import com.danyastry.nfcpay.feature.tags.state.TagResult

@Composable
fun TagsRoute(modifier: Modifier = Modifier) {
    val viewModel: TagsViewModel = viewModel(factory = viewModelFactory { initializer { TagsViewModel(SharedTag.tag) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nfc = rememberNfcAvailability()
    TagReaderEffect(enabled = state.waitingForTag && nfc.canUseRadio) { tag ->
        viewModel.onRealTag { NdefTagReader.read(tag) }
    }
    TagsScreen(
        state = state,
        nfc = nfc,
        actions = TagsActions(
            onSourceChange = viewModel::onSourceChange,
            onKindChange = viewModel::onKindChange,
            onInputChange = viewModel::onInputChange,
            onWrite = viewModel::onWrite,
            onRead = viewModel::onRead,
            onCancel = viewModel::onCancel,
        ),
        modifier = modifier,
    )
}

@Composable
fun TagsScreen(state: TagsUiState, nfc: NfcAvailability, actions: TagsActions, modifier: Modifier = Modifier) {
    val result = state.result
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { NfcStatusBanner(nfc) }
        item {
            SegmentedSelector(
                options = TagSource.entries,
                selected = state.source,
                label = { stringResource(if (it == TagSource.VIRTUAL) R.string.tags_source_virtual else R.string.tags_source_nfc) },
                onSelect = actions.onSourceChange,
            )
        }
        if (state.source == TagSource.VIRTUAL) {
            item { Editor(state, actions) }
        } else {
            item { RealTagControls(state, nfc, actions) }
        }
        if (result != null) {
            item { ResultCard(result) }
            if (result.exchanges.isNotEmpty()) {
                item { Text(stringResource(R.string.tags_exchanges, result.exchanges.size), style = MaterialTheme.typography.titleSmall) }
                items(result.exchanges) { ExchangeCard(it, mask = false, decodeTlv = false) }
            }
        }
    }
}

@Composable
private fun Editor(state: TagsUiState, actions: TagsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedSelector(
            options = RecordKind.entries,
            selected = state.kind,
            label = { stringResource(if (it == RecordKind.TEXT) R.string.tags_kind_text else R.string.tags_kind_link) },
            onSelect = actions.onKindChange,
        )
        OutlinedTextField(
            value = state.input,
            onValueChange = actions.onInputChange,
            label = { Text(stringResource(if (state.kind == RecordKind.TEXT) R.string.tags_input_text else R.string.tags_input_link)) },
            singleLine = state.kind == RecordKind.LINK,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = actions.onWrite, enabled = state.canWrite) { Text(stringResource(R.string.tags_write)) }
            OutlinedButton(onClick = actions.onRead, enabled = !state.isBusy) { Text(stringResource(R.string.tags_read)) }
            if (state.isBusy) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
        }
        Text(stringResource(R.string.tags_virtual_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RealTagControls(state: TagsUiState, nfc: NfcAvailability, actions: TagsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.waitingForTag) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.tags_waiting), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = actions.onCancel) { Text(stringResource(R.string.tags_cancel)) }
            }
        } else {
            Button(onClick = actions.onRead, enabled = nfc.canUseRadio && !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tags_read_real))
            }
        }
        if (!nfc.canUseRadio) {
            Text(stringResource(R.string.tags_nfc_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ResultCard(result: TagResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val title = when {
                result.error != null -> stringResource(R.string.tags_failed, result.error)
                result.action == TagAction.WRITE -> stringResource(R.string.tags_written)
                else -> stringResource(R.string.tags_read_done, result.records.size)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (result.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            result.records.forEach { RecordRow(it) }
            result.messageHex?.let {
                Text(stringResource(R.string.tags_message_bytes), style = MaterialTheme.typography.labelMedium)
                Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun RecordRow(record: RecordView) {
    Column {
        Text("${record.kind}: ${record.content}", style = MaterialTheme.typography.bodyLarge)
        Text(record.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
