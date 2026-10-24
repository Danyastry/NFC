package com.danyastry.nfcpay.feature.issuer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.danyastry.nfcpay.feature.issuer.ui.IssuerRoute

object IssuerFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "issuer-host",
        name = "Issuer host",
        category = FeatureCategory.CRYPTOGRAMS,
        summary = "The bank side of a tap: key derivation, ARQC check, replay protection and the ARPC answer",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        IssuerRoute(modifier = modifier)
    }
}
