package com.danyastry.nfcpay.feature.card

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.danyastry.nfcpay.feature.card.ui.CardRoute

object CardFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "card-emulation",
        name = "Card emulation",
        category = FeatureCategory.PAYMENTS,
        summary = "The virtual payment card behind host card emulation, with every APDU it has answered",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        CardRoute(modifier = modifier)
    }
}
