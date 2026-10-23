package com.danyastry.nfcpay.core.emv.terminal

data class AflEntry(val sfi: Int, val firstRecord: Int, val lastRecord: Int, val offlineAuthRecords: Int) {

    val records: IntRange
        get() = firstRecord..lastRecord

    companion object {
        fun parse(afl: ByteArray): List<AflEntry> {
            if (afl.isEmpty() || afl.size % 4 != 0) throw KernelException(TerminalError.BAD_AFL)
            return afl.toList().chunked(4).map { chunk ->
                val bytes = chunk.map { it.toInt() and 0xFF }
                val entry = AflEntry(bytes[0] ushr 3, bytes[1], bytes[2], bytes[3])
                val valid = entry.sfi in 1..30 && entry.firstRecord >= 1 && entry.lastRecord >= entry.firstRecord &&
                    entry.offlineAuthRecords <= entry.lastRecord - entry.firstRecord + 1
                if (!valid) throw KernelException(TerminalError.BAD_AFL)
                entry
            }
        }
    }
}
