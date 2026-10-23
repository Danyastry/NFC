package com.danyastry.nfcpay.feature.card.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.nfc.hce.ApduSource
import com.danyastry.nfcpay.core.nfc.hce.CardEmulationStatus
import com.danyastry.nfcpay.core.nfc.hce.SharedCard
import com.danyastry.nfcpay.core.nfc.radio.NfcAvailability
import com.danyastry.nfcpay.core.nfc.ui.NfcStatusBanner
import com.danyastry.nfcpay.core.nfc.ui.rememberNfcAvailability
import com.danyastry.nfcpay.feature.card.R
import com.danyastry.nfcpay.feature.card.model.CardDetails
import com.danyastry.nfcpay.feature.card.model.ServedEntry

@Composable
fun CardRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: CardViewModel = viewModel(factory = viewModelFactory { initializer { CardViewModel(SharedCard.host) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nfc = rememberNfcAvailability()
    var checks by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        checks++
        onPauseOrDispose {}
    }
    val isDefault = remember(checks, nfc) { CardEmulationStatus.isDefaultPaymentService(context) }
    CardScreen(
        state = state,
        nfc = nfc,
        isDefaultPaymentService = isDefault,
        actions = CardActions(
            onResetCounter = viewModel::onResetCounter,
            onClearLog = viewModel::onClearLog,
            onMakeDefault = { context.startActivity(CardEmulationStatus.requestDefaultIntent(context)) },
        ),
        modifier = modifier,
    )
}

@Composable
fun CardScreen(
    state: CardUiState,
    nfc: NfcAvailability,
    isDefaultPaymentService: Boolean,
    actions: CardActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { NfcStatusBanner(nfc) }
        item { VirtualCardFace(state.details) }
        item { CounterRow(state.atc, actions) }
        item { EmulationStatus(nfc, isDefaultPaymentService, actions) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.card_log_title, state.log.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onClearLog, enabled = state.log.isNotEmpty()) { Text(stringResource(R.string.card_clear_log)) }
            }
        }
        if (state.log.isEmpty()) {
            item {
                Text(stringResource(R.string.card_log_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.log, key = { "${it.timeMillis}-${it.command}-${it.response}" }) { ServedRow(it) }
    }
}

@Composable
private fun VirtualCardFace(details: CardDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(details.label, style = MaterialTheme.typography.titleMedium)
            Text(details.pan, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(details.cardholderName, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.card_expiry, details.expiry), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.card_aid, details.aid), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.card_test_only), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CounterRow(atc: Int, actions: CardActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.card_atc, atc), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.card_atc_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = actions.onResetCounter) { Text(stringResource(R.string.card_reset_counter)) }
    }
}

@Composable
private fun EmulationStatus(nfc: NfcAvailability, isDefault: Boolean, actions: CardActions) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.card_hce_title), style = MaterialTheme.typography.titleSmall)
            val canEmulate = nfc is NfcAvailability.Ready && nfc.canEmulateCards
            when {
                !canEmulate -> Text(stringResource(R.string.card_hce_unavailable), style = MaterialTheme.typography.bodySmall)
                isDefault -> Text(stringResource(R.string.card_hce_default), style = MaterialTheme.typography.bodySmall)
                else -> {
                    Text(stringResource(R.string.card_hce_not_default), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = actions.onMakeDefault) { Text(stringResource(R.string.card_make_default)) }
                }
            }
        }
    }
}

@Composable
private fun ServedRow(entry: ServedEntry) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(stringResource(entry.source.labelRes)) })
            }
            entry.sw?.let {
                Text(
                    stringResource(R.string.card_sw, "%04X".format(it), StatusWord.describe(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == StatusWord.SUCCESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            Text(stringResource(R.string.card_command, entry.command), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.card_response, entry.response), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

private val ApduSource.labelRes: Int
    get() = when (this) {
        ApduSource.SIMULATED -> R.string.card_source_simulated
        ApduSource.NFC -> R.string.card_source_nfc
    }
