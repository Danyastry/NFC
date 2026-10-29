package com.danyastry.nfcpay.feature.tokens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.danyastry.nfcpay.core.nfc.wallet.SharedWallet
import com.danyastry.nfcpay.feature.tokens.R
import com.danyastry.nfcpay.feature.tokens.model.VaultEntry
import com.danyastry.nfcpay.feature.tokens.model.WalletView

@Composable
fun TokensRoute(modifier: Modifier = Modifier) {
    val viewModel: TokensViewModel = viewModel(factory = viewModelFactory { initializer { TokensViewModel(SharedWallet.wallet) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    TokensScreen(
        state = state,
        actions = TokensActions(
            onAddCard = viewModel::onAddCard,
            onReplenish = viewModel::onReplenish,
            onSuspend = viewModel::onSuspend,
            onResume = viewModel::onResume,
            onRemove = viewModel::onRemove,
        ),
        modifier = modifier,
    )
}

@Composable
fun TokensScreen(state: TokensUiState, actions: TokensActions, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val wallet = state.wallet
        if (wallet == null) {
            item { EmptyWallet(actions) }
        } else {
            item { TokenFace(wallet) }
            item { KeyCard(wallet, actions) }
            item { Lifecycle(wallet, actions) }
        }
        if (state.vault.isNotEmpty()) {
            item { Text(stringResource(R.string.tokens_vault, state.vault.size), style = MaterialTheme.typography.titleSmall) }
            items(state.vault, key = { it.token }) { VaultRow(it) }
        }
    }
}

@Composable
private fun EmptyWallet(actions: TokensActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tokens_intro), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = actions.onAddCard, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.tokens_add)) }
    }
}

@Composable
private fun TokenFace(wallet: WalletView) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.tokens_wallet_title, wallet.status), style = MaterialTheme.typography.titleMedium)
            Text(wallet.token, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.tokens_for_account, wallet.account, wallet.expiry), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.tokens_par, wallet.paymentAccountReference), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text(stringResource(R.string.tokens_requestor, wallet.requestorId), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.tokens_explain), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun KeyCard(wallet: WalletView, actions: TokensActions) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tokens_key_title, wallet.keyIndex, wallet.keyCheckValue), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.tokens_taps_left, wallet.tapsLeft, wallet.tapsPerKey), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { wallet.tapsLeft.toFloat() / wallet.tapsPerKey },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.tokens_atc, wallet.atc), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.tokens_key_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = actions.onReplenish, enabled = wallet.isActive) { Text(stringResource(R.string.tokens_replenish)) }
        }
    }
}

@Composable
private fun Lifecycle(wallet: WalletView, actions: TokensActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tokens_lifecycle), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (wallet.isSuspended) {
                OutlinedButton(onClick = actions.onResume) { Text(stringResource(R.string.tokens_resume)) }
            } else {
                OutlinedButton(onClick = actions.onSuspend) { Text(stringResource(R.string.tokens_suspend)) }
            }
            OutlinedButton(onClick = actions.onRemove) { Text(stringResource(R.string.tokens_remove)) }
        }
        Text(stringResource(R.string.tokens_pay_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VaultRow(entry: VaultEntry) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(entry.token, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text("${entry.status} - ${entry.domain}", style = MaterialTheme.typography.bodySmall)
    }
}
