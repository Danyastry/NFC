package com.danyastry.nfcpay.feature.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.danyastry.nfcpay.feature.terminal.ui.TerminalRoute

object TerminalFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "payment-terminal",
        name = "Payment terminal",
        category = FeatureCategory.PAYMENTS,
        summary = "A SoftPOS-style EMV kernel: select, processing options, records and an ARQC, step by step",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        TerminalRoute(modifier = modifier)
    }
}
