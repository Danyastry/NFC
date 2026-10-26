package com.danyastry.nfcpay.catalog

import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.feature.card.CardFeature
import com.danyastry.nfcpay.feature.issuer.IssuerFeature
import com.danyastry.nfcpay.feature.tags.TagsFeature
import com.danyastry.nfcpay.feature.terminal.TerminalFeature
import com.danyastry.nfcpay.feature.tlv.TlvFeature
import com.danyastry.nfcpay.feature.tokens.TokensFeature

object FeatureRegistry {
    val features: List<FeatureEntry> = listOf(
        TerminalFeature,
        CardFeature,
        TlvFeature,
        IssuerFeature,
        TokensFeature,
        TagsFeature,
    )
}
