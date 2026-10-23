package com.danyastry.nfcpay.core.ui

import androidx.annotation.StringRes

@get:StringRes
val FeatureCategory.titleRes: Int
    get() = when (this) {
        FeatureCategory.PAYMENTS -> R.string.ui_category_payments
        FeatureCategory.CRYPTOGRAMS -> R.string.ui_category_cryptograms
        FeatureCategory.KEY_MANAGEMENT -> R.string.ui_category_key_management
        FeatureCategory.TAGS -> R.string.ui_category_tags
        FeatureCategory.ATTACKS -> R.string.ui_category_attacks
    }

@get:StringRes
val FeatureCategory.descriptionRes: Int
    get() = when (this) {
        FeatureCategory.PAYMENTS -> R.string.ui_category_payments_description
        FeatureCategory.CRYPTOGRAMS -> R.string.ui_category_cryptograms_description
        FeatureCategory.KEY_MANAGEMENT -> R.string.ui_category_key_management_description
        FeatureCategory.TAGS -> R.string.ui_category_tags_description
        FeatureCategory.ATTACKS -> R.string.ui_category_attacks_description
    }
