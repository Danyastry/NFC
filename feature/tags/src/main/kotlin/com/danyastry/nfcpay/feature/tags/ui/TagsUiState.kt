package com.danyastry.nfcpay.feature.tags.ui

import com.danyastry.nfcpay.feature.tags.model.RecordKind
import com.danyastry.nfcpay.feature.tags.model.TagSource
import com.danyastry.nfcpay.feature.tags.state.TagResult

data class TagsUiState(
    val source: TagSource = TagSource.VIRTUAL,
    val kind: RecordKind = RecordKind.TEXT,
    val input: String = "",
    val isBusy: Boolean = false,
    val waitingForTag: Boolean = false,
    val result: TagResult? = null,
) {
    val canWrite: Boolean
        get() = source == TagSource.VIRTUAL && input.isNotBlank() && !isBusy
}
