package com.danyastry.nfcpay.core.nfc.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danyastry.nfcpay.core.nfc.R
import com.danyastry.nfcpay.core.nfc.radio.NfcAvailability
import com.danyastry.nfcpay.core.nfc.radio.NfcStatus

@Composable
fun rememberNfcAvailability(): NfcAvailability {
    val context = LocalContext.current
    val state by remember(context) { NfcStatus.changes(context) }
        .collectAsStateWithLifecycle(initialValue = NfcStatus.current(context))
    return NfcAvailability.resolve(state)
}

@Composable
fun NfcStatusBanner(availability: NfcAvailability, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val text = when (availability) {
        NfcAvailability.NoNfc -> stringResource(R.string.nfc_status_no_nfc)
        NfcAvailability.Off -> stringResource(R.string.nfc_status_off)
        is NfcAvailability.Ready -> if (availability.canEmulateCards) {
            stringResource(R.string.nfc_status_ready)
        } else {
            stringResource(R.string.nfc_status_ready_without_hce)
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (availability.canUseRadio) colors.secondaryContainer else colors.surfaceVariant,
        contentColor = if (availability.canUseRadio) colors.onSecondaryContainer else colors.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (availability == NfcAvailability.Off) {
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) {
                    Text(stringResource(R.string.nfc_status_open_settings))
                }
            }
        }
    }
}
