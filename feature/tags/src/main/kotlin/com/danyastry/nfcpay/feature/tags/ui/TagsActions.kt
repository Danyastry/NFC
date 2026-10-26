package com.danyastry.nfcpay.feature.tags.ui

import com.danyastry.nfcpay.feature.tags.model.RecordKind
import com.danyastry.nfcpay.feature.tags.model.TagSource

data class TagsActions(
    val onSourceChange: (TagSource) -> Unit,
    val onKindChange: (RecordKind) -> Unit,
    val onInputChange: (String) -> Unit,
    val onWrite: () -> Unit,
    val onRead: () -> Unit,
    val onCancel: () -> Unit,
)
