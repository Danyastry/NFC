package com.danyastry.nfcpay.feature.tlv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.danyastry.nfcpay.feature.tlv.ui.TlvRoute

object TlvFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "tlv-decoder",
        name = "TLV decoder",
        category = FeatureCategory.PAYMENTS,
        summary = "Paste BER-TLV bytes from a card or a field 55 and read them tag by tag",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        TlvRoute(modifier = modifier)
    }
}
