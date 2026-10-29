package com.danyastry.nfcpay.feature.terminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danyastry.nfcpay.core.emv.pin.IsoPinBlock
import com.danyastry.nfcpay.feature.terminal.R
import com.danyastry.nfcpay.feature.terminal.state.PinPrompt

@Composable
fun PinPadDialog(prompt: PinPrompt, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var digits by remember(prompt) { mutableStateOf("") }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(dismissOnClickOutside = false)) {
        Card {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.terminal_pin_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.terminal_pin_subtitle, prompt.amount, prompt.maskedPan), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "*".repeat(digits.length).ifEmpty { "-" },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                KEY_ROWS.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { key ->
                            PadKey(key, Modifier.weight(1f)) {
                                when (key) {
                                    CLEAR -> digits = ""
                                    OK -> if (digits.length >= IsoPinBlock.MIN_PIN) onSubmit(digits)
                                    else -> if (digits.length < IsoPinBlock.MAX_PIN) digits += key
                                }
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.terminal_pin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.terminal_cancel)) }
            }
        }
    }
}

@Composable
private fun PadKey(key: String, modifier: Modifier, onClick: () -> Unit) {
    when (key) {
        OK -> Button(onClick = onClick, modifier = modifier) { Text(stringResource(R.string.terminal_pin_ok)) }
        CLEAR -> FilledTonalButton(onClick = onClick, modifier = modifier) { Text(stringResource(R.string.terminal_pin_clear)) }
        else -> FilledTonalButton(onClick = onClick, modifier = modifier) { Text(key, style = MaterialTheme.typography.titleMedium) }
    }
}

private const val CLEAR = "clear"
private const val OK = "ok"
private val KEY_ROWS = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(CLEAR, "0", OK))
