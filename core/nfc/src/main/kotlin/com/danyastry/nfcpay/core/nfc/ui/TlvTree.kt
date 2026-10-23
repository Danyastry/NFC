package com.danyastry.nfcpay.core.nfc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.tags.EmvDictionary
import com.danyastry.nfcpay.core.emv.tags.TagText
import com.danyastry.nfcpay.core.emv.tlv.Tlv

@Composable
fun TlvTree(tlvs: List<Tlv>, mask: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        tlvs.forEach { TlvNode(it, mask, depth = 0) }
    }
}

@Composable
private fun TlvNode(tlv: Tlv, mask: Boolean, depth: Int) {
    Column(modifier = Modifier.padding(start = (depth * INDENT_DP).dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            text = "${tlv.tag.hex}  ${EmvDictionary.nameOf(tlv.tag)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (tlv.isConstructed && tlv.children.isNotEmpty()) {
            tlv.children.forEach { TlvNode(it, mask, depth + 1) }
        } else {
            val hidden = mask && EmvDictionary.isSensitive(tlv.tag)
            Text(
                text = SensitiveData.displayValue(tlv, mask),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TagText.of(tlv)?.takeUnless { hidden }?.let {
                Text(text = "\"$it\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private const val INDENT_DP = 12
