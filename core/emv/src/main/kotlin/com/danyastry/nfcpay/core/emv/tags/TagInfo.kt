package com.danyastry.nfcpay.core.emv.tags

data class TagInfo(
    val name: String,
    val format: TagFormat,
    val isSensitive: Boolean = false,
)
