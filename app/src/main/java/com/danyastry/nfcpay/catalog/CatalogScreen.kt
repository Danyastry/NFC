package com.danyastry.nfcpay.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danyastry.nfcpay.R
import com.danyastry.nfcpay.core.nfc.radio.NfcAvailability
import com.danyastry.nfcpay.core.nfc.ui.NfcStatusBanner
import com.danyastry.nfcpay.core.nfc.ui.rememberNfcAvailability
import com.danyastry.nfcpay.core.ui.AndroidVersion
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.RequirementBadge
import com.danyastry.nfcpay.core.ui.descriptionRes
import com.danyastry.nfcpay.core.ui.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    catalog: FeatureCatalog,
    onFeatureClick: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        val nfc = rememberNfcAvailability()
        if (catalog.isEmpty) {
            EmptyCatalog(nfc, Modifier.padding(innerPadding))
        } else {
            CatalogList(catalog, nfc, onFeatureClick, innerPadding)
        }
    }
}

@Composable
private fun CatalogList(
    catalog: FeatureCatalog,
    nfc: NfcAvailability,
    onFeatureClick: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(contentPadding = contentPadding) {
        item(key = NFC_STATUS_KEY) {
            NfcStatusBanner(nfc, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        catalog.sections.forEach { section ->
            item(key = section.category.name) {
                SectionHeader(section.category)
            }
            items(section.entries, key = { it.info.id }) { entry ->
                FeatureRow(
                    entry = entry,
                    isSupported = catalog.isSupported(entry),
                    onClick = { onFeatureClick(entry.info.id) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(category: FeatureCategory) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)) {
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(category.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeatureRow(entry: FeatureEntry, isSupported: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.info.name) },
        supportingContent = { Text(entry.info.summary) },
        trailingContent = if (entry.info.minSdk > AndroidVersion.BASELINE_SDK) {
            { RequirementBadge(minSdk = entry.info.minSdk, isSupported = isSupported) }
        } else {
            null
        },
        modifier = Modifier
            .alpha(if (isSupported) 1f else UNSUPPORTED_ALPHA)
            .clickable(enabled = isSupported, onClick = onClick),
    )
}

@Composable
private fun EmptyCatalog(nfc: NfcAvailability, modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        NfcStatusBanner(nfc, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.catalog_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val UNSUPPORTED_ALPHA = 0.5f
private const val NFC_STATUS_KEY = "nfc-status"
