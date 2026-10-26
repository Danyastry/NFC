package com.danyastry.nfcpay.core.nfc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.terminal.Exchange
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.nfc.R

@Composable
fun ExchangeCard(exchange: Exchange, mask: Boolean, modifier: Modifier = Modifier, decodeTlv: Boolean = true) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tlvs = remember(exchange, decodeTlv) {
        if (decodeTlv) runCatching { BerTlv.parse(exchange.response.data) }.getOrDefault(emptyList()) else emptyList()
    }
    val hideRaw = mask && SensitiveData.containsSensitive(tlvs)
    val status = exchange.response.sw
    OutlinedCard(modifier = modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = exchange.label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.nfc_exchange_status, "%04X".format(status), StatusWord.describe(status)),
                style = MaterialTheme.typography.bodySmall,
                color = if (exchange.response.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (expanded) {
                MonoLine(stringResource(R.string.nfc_exchange_command), exchange.command.encode().toHex())
                if (hideRaw) {
                    Text(stringResource(R.string.nfc_exchange_hidden), style = MaterialTheme.typography.bodySmall)
                } else if (exchange.response.data.isNotEmpty()) {
                    MonoLine(stringResource(R.string.nfc_exchange_response), exchange.response.data.toHex())
                }
                if (tlvs.isNotEmpty()) TlvTree(tlvs, mask, Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun MonoLine(label: String, hex: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = hex, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
