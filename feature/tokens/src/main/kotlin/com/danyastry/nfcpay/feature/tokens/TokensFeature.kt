package com.danyastry.nfcpay.feature.tokens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.danyastry.nfcpay.feature.tokens.ui.TokensRoute

object TokensFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "tokens",
        name = "Tokens",
        category = FeatureCategory.CRYPTOGRAMS,
        summary = "Put the card in a phone wallet: a token instead of the card number and keys that last a few taps",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        TokensRoute(modifier = modifier)
    }
}
