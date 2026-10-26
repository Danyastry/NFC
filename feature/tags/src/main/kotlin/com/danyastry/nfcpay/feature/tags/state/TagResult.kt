package com.danyastry.nfcpay.feature.tags.state

import com.danyastry.nfcpay.core.emv.terminal.Exchange
import com.danyastry.nfcpay.feature.tags.model.RecordView
import com.danyastry.nfcpay.feature.tags.model.TagAction
import com.danyastry.nfcpay.feature.tags.model.TagSource

data class TagResult(
    val source: TagSource,
    val action: TagAction,
    val records: List<RecordView>,
    val messageHex: String?,
    val error: String?,
    val exchanges: List<Exchange>,
)
