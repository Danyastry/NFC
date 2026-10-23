package com.danyastry.nfcpay.core.emv.terminal

data class Tvr(val bits: Set<TvrBit> = emptySet()) {

    operator fun plus(bit: TvrBit): Tvr = Tvr(bits + bit)

    fun encode(): ByteArray {
        val bytes = ByteArray(SIZE)
        bits.forEach { bytes[it.byteIndex] = (bytes[it.byteIndex].toInt() or it.mask).toByte() }
        return bytes
    }

    companion object {
        const val SIZE = 5

        fun decode(bytes: ByteArray): Tvr {
            require(bytes.size == SIZE) { "TVR is $SIZE bytes, got ${bytes.size}" }
            return Tvr(TvrBit.entries.filter { bytes[it.byteIndex].toInt() and it.mask != 0 }.toSet())
        }
    }
}
