package com.danyastry.nfcpay.feature.tlv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danyastry.nfcpay.core.nfc.ui.TlvTree
import com.danyastry.nfcpay.feature.tlv.R
import com.danyastry.nfcpay.feature.tlv.model.TlvDecodeResult
import com.danyastry.nfcpay.feature.tlv.model.TlvSample

@Composable
fun TlvRoute(modifier: Modifier = Modifier) {
    val viewModel: TlvViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TlvScreen(
        state = state,
        actions = TlvActions(
            onInputChange = viewModel::onInputChange,
            onSample = viewModel::onSample,
            onClear = viewModel::onClear,
        ),
        modifier = modifier,
    )
}

@Composable
fun TlvScreen(state: TlvUiState, actions: TlvActions, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.input,
                onValueChange = actions.onInputChange,
                label = { Text(stringResource(R.string.tlv_input)) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                minLines = 3,
                isError = state.result is TlvDecodeResult.Invalid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tlv_samples), style = MaterialTheme.typography.labelMedium)
                TlvSample.entries.forEach { sample ->
                    AssistChip(onClick = { actions.onSample(sample) }, label = { Text(sample.label) })
                }
                TextButton(onClick = actions.onClear) { Text(stringResource(R.string.tlv_clear)) }
            }
        }
        item {
            when (val result = state.result) {
                TlvDecodeResult.Empty -> Text(
                    stringResource(R.string.tlv_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is TlvDecodeResult.Invalid -> Text(
                    stringResource(R.string.tlv_invalid, result.reason),
                    color = MaterialTheme.colorScheme.error,
                )
                is TlvDecodeResult.Decoded -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tlv_summary, result.byteCount, result.tlvs.size), style = MaterialTheme.typography.titleSmall)
                    TlvTree(result.tlvs, mask = false)
                }
            }
        }
    }
}
