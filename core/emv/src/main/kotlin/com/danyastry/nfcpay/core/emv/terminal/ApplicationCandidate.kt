package com.danyastry.nfcpay.core.emv.terminal

data class ApplicationCandidate(
    val aid: ByteArray,
    val label: String?,
    val priority: Int,
) {
    override fun equals(other: Any?): Boolean =
        other is ApplicationCandidate && aid.contentEquals(other.aid) && label == other.label && priority == other.priority

    override fun hashCode(): Int = aid.contentHashCode()
}
