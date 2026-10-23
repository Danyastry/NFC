package com.danyastry.nfcpay.catalog

import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.feature.terminal.TerminalFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        TerminalFeature,
    )
}
