package com.danyastry.nfcpay.feature.issuer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.issuer.VerificationStep
import com.danyastry.nfcpay.core.nfc.issuer.SharedIssuer
import com.danyastry.nfcpay.feature.issuer.R
import com.danyastry.nfcpay.feature.issuer.model.AccountView
import com.danyastry.nfcpay.feature.issuer.model.AuthorizationView
import com.danyastry.nfcpay.feature.issuer.model.KeyHierarchy

@Composable
fun IssuerRoute(modifier: Modifier = Modifier) {
    val viewModel: IssuerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { IssuerViewModel(SharedIssuer.link, DemoCards.issuerMasterKey, DemoIssuer.account()) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    IssuerScreen(state, IssuerActions(onReplay = viewModel::onReplay, onReset = viewModel::onReset), modifier)
}

@Composable
fun IssuerScreen(state: IssuerUiState, actions: IssuerActions, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.account?.let { item { AccountCard(it) } }
        item { KeysCard(state.keys) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions.onReplay, enabled = state.canReplay) { Text(stringResource(R.string.issuer_replay)) }
                OutlinedButton(onClick = actions.onReset) { Text(stringResource(R.string.issuer_reset)) }
            }
        }
        item {
            Text(stringResource(R.string.issuer_log_title, state.authorizations.size), style = MaterialTheme.typography.titleSmall)
        }
        if (state.authorizations.isEmpty()) {
            item {
                Text(stringResource(R.string.issuer_log_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.authorizations, key = { "${it.timeMillis}-${it.atc}-${it.isReplay}-${it.responseCode}" }) { AuthorizationCard(it) }
    }
}

@Composable
private fun AccountCard(account: AccountView) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.issuer_account), style = MaterialTheme.typography.labelMedium)
            Text(account.pan, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.issuer_balance, account.balance), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.issuer_last_atc, account.lastAtc), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun KeysCard(keys: KeyHierarchy) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.issuer_keys), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.issuer_master_key, keys.issuerMasterKeyCheck), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.issuer_card_key, keys.cardKeyCheck, keys.cardKeyDerivation), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.issuer_session_key), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.issuer_keys_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AuthorizationCard(view: AuthorizationView) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.issuer_response, view.responseCode, view.description),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (view.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                if (view.isReplay) Text(stringResource(R.string.issuer_replayed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            val details = listOfNotNull(
                view.amount?.let { stringResource(R.string.issuer_amount, it) },
                view.atc?.let { stringResource(R.string.issuer_atc, it) },
            )
            if (details.isNotEmpty()) Text(details.joinToString(" - "), style = MaterialTheme.typography.bodySmall)
            view.steps.forEach { StepRow(it) }
        }
    }
}

@Composable
private fun StepRow(step: VerificationStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = if (step.passed) Icons.Default.Check else Icons.Default.Close,
            contentDescription = stringResource(if (step.passed) R.string.issuer_step_passed else R.string.issuer_step_failed),
            tint = if (step.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Column {
            Text(step.title, style = MaterialTheme.typography.labelLarge)
            Text(step.detail, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
