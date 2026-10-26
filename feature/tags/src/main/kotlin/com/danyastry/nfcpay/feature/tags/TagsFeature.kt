package com.danyastry.nfcpay.feature.tags

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.danyastry.nfcpay.feature.tags.ui.TagsRoute

object TagsFeature : FeatureEntry {

    override val info = FeatureInfo(
        id = "ndef-tags",
        name = "NDEF tags",
        category = FeatureCategory.TAGS,
        summary = "Write a text or a link to a Type 4 tag and read it back, APDU by APDU",
    )

    @Composable
    override fun Content(modifier: Modifier) {
        TagsRoute(modifier = modifier)
    }
}
