package com.danyastry.nfcpay.feature.terminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.issuer.ResponseCode
import com.danyastry.nfcpay.core.emv.terminal.OnlineResponse
import com.danyastry.nfcpay.core.emv.terminal.OnlineResult
import com.danyastry.nfcpay.core.emv.terminal.TerminalError
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.nfc.hce.ApduSource
import com.danyastry.nfcpay.core.nfc.hce.SharedCard
import com.danyastry.nfcpay.core.nfc.issuer.SharedIssuer
import com.danyastry.nfcpay.core.nfc.radio.NfcAvailability
import com.danyastry.nfcpay.core.nfc.reader.IsoDepTransport
import com.danyastry.nfcpay.core.nfc.pin.SharedPin
import com.danyastry.nfcpay.core.nfc.reader.ReaderModeEffect
import com.danyastry.nfcpay.core.nfc.wallet.SharedWallet
import com.danyastry.nfcpay.core.nfc.ui.ExchangeCard
import com.danyastry.nfcpay.core.nfc.ui.NfcStatusBanner
import com.danyastry.nfcpay.core.nfc.ui.rememberNfcAvailability
import com.danyastry.nfcpay.core.ui.components.SegmentedSelector
import com.danyastry.nfcpay.feature.terminal.R
import com.danyastry.nfcpay.feature.terminal.logic.AmountInput
import com.danyastry.nfcpay.feature.terminal.model.CardSource
import com.danyastry.nfcpay.feature.terminal.model.TerminalPhase
import com.danyastry.nfcpay.feature.terminal.state.CompletedTap
import java.time.format.DateTimeFormatter

@Composable
fun TerminalRoute(modifier: Modifier = Modifier) {
    val viewModel: TerminalViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TerminalViewModel(
                    simulatedCard = SharedCard.host.handler(ApduSource.SIMULATED),
                    authorizer = SharedIssuer.link.authorizer,
                    walletCard = SharedWallet.wallet::handler,
                    encryptPin = SharedPin.device::encryptPin,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wallet by SharedWallet.wallet.state.collectAsStateWithLifecycle()
    val nfc = rememberNfcAvailability()
    ReaderModeEffect(enabled = state.phase == TerminalPhase.WAITING_FOR_CARD && nfc.canUseRadio) { isoDep ->
        viewModel.onCardPresented { IsoDepTransport.open(isoDep) }
    }
    TerminalScreen(
        state = state,
        nfc = nfc,
        walletReady = wallet != null,
        actions = TerminalActions(
            onSourceChange = viewModel::onSourceChange,
            onAmountChange = viewModel::onAmountChange,
            onTap = viewModel::onTap,
            onCancel = viewModel::onCancel,
            onPinSubmit = viewModel::onPinSubmit,
            onPinCancel = viewModel::onPinCancel,
        ),
        modifier = modifier,
    )
}

@Composable
fun TerminalScreen(
    state: TerminalUiState,
    nfc: NfcAvailability,
    walletReady: Boolean,
    actions: TerminalActions,
    modifier: Modifier = Modifier,
) {
    val tap = state.lastTap
    state.pinPrompt?.let { PinPadDialog(it, onSubmit = actions.onPinSubmit, onCancel = actions.onPinCancel) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { NfcStatusBanner(nfc) }
        item {
            SegmentedSelector(
                options = CardSource.entries,
                selected = state.source,
                label = { stringResource(it.labelRes) },
                onSelect = actions.onSourceChange,
            )
        }
        item { AmountField(state, actions) }
        item { TapControls(state, nfc, walletReady, actions) }
        if (tap != null) {
            item { OutcomeCard(tap) }
            item {
                Column {
                    Text(stringResource(R.string.terminal_exchanges, tap.result.exchanges.size), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.terminal_exchanges_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(tap.result.exchanges) { exchange -> ExchangeCard(exchange, mask = tap.maskCardData) }
        }
    }
}

@Composable
private fun AmountField(state: TerminalUiState, actions: TerminalActions) {
    val invalid = state.amount == null
    OutlinedTextField(
        value = state.amountText,
        onValueChange = actions.onAmountChange,
        label = { Text(stringResource(R.string.terminal_amount)) },
        suffix = { Text(stringResource(R.string.terminal_currency)) },
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) {
            { Text(stringResource(R.string.terminal_amount_invalid)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        enabled = state.phase == TerminalPhase.IDLE,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TapControls(state: TerminalUiState, nfc: NfcAvailability, walletReady: Boolean, actions: TerminalActions) {
    val needsRadio = state.source == CardSource.NFC && !nfc.canUseRadio
    val needsWallet = state.source == CardSource.WALLET && !walletReady
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state.phase) {
            TerminalPhase.IDLE -> Button(
                onClick = actions.onTap,
                enabled = state.canTap && !needsRadio && !needsWallet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.source == CardSource.NFC) R.string.terminal_wait else R.string.terminal_tap))
            }
            TerminalPhase.WAITING_FOR_CARD -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.terminal_waiting), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = actions.onCancel) { Text(stringResource(R.string.terminal_cancel)) }
            }
            TerminalPhase.READING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.terminal_reading))
            }
        }
        if (needsWallet) {
            Text(stringResource(R.string.terminal_wallet_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (needsRadio) {
            Text(stringResource(R.string.terminal_nfc_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(stringResource(R.string.terminal_no_charge), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OutcomeCard(tap: CompletedTap) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (val outcome = tap.result.outcome) {
                is TransactionOutcome.Completed -> CompletedDetails(outcome, tap)
                is TransactionOutcome.Failed -> {
                    Text(
                        stringResource(R.string.terminal_failed, stringResource(outcome.error.messageRes)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    outcome.sw?.let { Text(stringResource(R.string.terminal_failed_sw, "%04X".format(it))) }
                    if (tap.source == CardSource.WALLET && outcome.error == TerminalError.PROCESSING_OPTIONS_REFUSED) {
                        Text(stringResource(R.string.terminal_wallet_out_of_keys), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedDetails(outcome: TransactionOutcome.Completed, tap: CompletedTap) {
    val card = outcome.card
    val pan = if (tap.maskCardData) SensitiveData.maskPan(card.pan) else card.pan
    Text(
        text = "${AmountInput.format(tap.amount)} ${stringResource(R.string.terminal_currency)}",
        style = MaterialTheme.typography.headlineSmall,
    )
    outcome.online?.let { OnlineSummary(it) }
    Text(stringResource(outcome.decision.decisionRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.terminal_card, listOfNotNull(card.scheme?.label, card.label).distinctBy { it.lowercase() }.joinToString(" · ")))
    Text(stringResource(R.string.terminal_pan, SensitiveData.groupPan(pan)), fontFamily = FontFamily.Monospace)
    Text(
        stringResource(
            R.string.terminal_expiry,
            card.expiry?.format(EXPIRY_FORMAT) ?: stringResource(R.string.terminal_expiry_unknown),
        ),
    )
    Text(stringResource(R.string.terminal_aid, card.aid), fontFamily = FontFamily.Monospace)
    Text(stringResource(R.string.terminal_cryptogram, outcome.cryptogram.value), fontFamily = FontFamily.Monospace)
    Text(stringResource(R.string.terminal_atc, outcome.cryptogram.atc))
    if (outcome.cryptogram.fromProcessingOptions) {
        Text(stringResource(R.string.terminal_from_gpo), style = MaterialTheme.typography.bodySmall)
    }
    val flags = outcome.tvr.bits.sortedBy { it.ordinal }.joinToString("; ") { it.description }
    Text(stringResource(R.string.terminal_tvr, flags.ifEmpty { stringResource(R.string.terminal_tvr_clean) }), style = MaterialTheme.typography.bodySmall)
    if (outcome.cardholderVerificationRequired) {
        Text(stringResource(R.string.terminal_cvm_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
    }
    Text(stringResource(R.string.terminal_field55), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    Text(
        text = outcome.field55,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun OnlineSummary(online: OnlineResult) {
    Text(
        text = stringResource(if (online.isApproved) R.string.terminal_approved else R.string.terminal_declined),
        style = MaterialTheme.typography.titleLarge,
        color = if (online.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Text(stringResource(R.string.terminal_issuer_answer, online.responseCode, responseDescription(online.responseCode)))
    online.finalCryptogram?.let {
        Text(stringResource(R.string.terminal_final_cryptogram, it.type.label, it.value), fontFamily = FontFamily.Monospace)
    }
    if (online.cardOverruledIssuer) {
        Text(stringResource(R.string.terminal_card_overruled), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun responseDescription(code: String): String = when (code) {
    OnlineResponse.UNABLE_TO_GO_ONLINE -> stringResource(R.string.terminal_unable_online)
    else -> ResponseCode.entries.firstOrNull { it.code == code }?.description ?: stringResource(R.string.terminal_unknown_code)
}

private val EXPIRY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yy")
