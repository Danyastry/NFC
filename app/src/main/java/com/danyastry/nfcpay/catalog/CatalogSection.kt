package com.danyastry.nfcpay.catalog

import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry

data class CatalogSection(
    val category: FeatureCategory,
    val entries: List<FeatureEntry>,
)
