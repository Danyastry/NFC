package com.danyastry.nfcpay.core.emv.apdu

import com.danyastry.nfcpay.core.emv.util.toHex

class CommandApdu(
    val cla: Int,
    val ins: Int,
    val p1: Int,
    val p2: Int,
    val data: ByteArray = ByteArray(0),
    val le: Int? = null,
) {
    init {
        listOf(cla, ins, p1, p2).forEach { require(it in 0..0xFF) { "Header bytes must fit in a byte, got $it" } }
        require(data.size <= MAX_SHORT_DATA) { "Short APDUs carry at most $MAX_SHORT_DATA data bytes, got ${data.size}" }
        require(le == null || le in 0..MAX_SHORT_LE) { "Le must be 0..$MAX_SHORT_LE, got $le" }
    }

    val instruction: Instruction?
        get() = Instruction.of(cla, ins)

    fun encode(): ByteArray {
        val header = byteArrayOf(cla.toByte(), ins.toByte(), p1.toByte(), p2.toByte())
        val body = if (data.isEmpty()) ByteArray(0) else byteArrayOf(data.size.toByte()) + data
        val trailer = le?.let { byteArrayOf((if (it == MAX_SHORT_LE) 0 else it).toByte()) } ?: ByteArray(0)
        return header + body + trailer
    }

    override fun equals(other: Any?): Boolean =
        other is CommandApdu && cla == other.cla && ins == other.ins && p1 == other.p1 && p2 == other.p2 &&
            data.contentEquals(other.data) && le == other.le

    override fun hashCode(): Int = encode().contentHashCode()

    override fun toString(): String = encode().toHex()

    companion object {
        const val MAX_SHORT_DATA = 255
        const val MAX_SHORT_LE = 256

        fun parse(bytes: ByteArray): CommandApdu {
            if (bytes.size < 4) throw ApduFormatException("A command needs a 4-byte header, got ${bytes.size} bytes")
            val cla = bytes[0].toInt() and 0xFF
            val ins = bytes[1].toInt() and 0xFF
            val p1 = bytes[2].toInt() and 0xFF
            val p2 = bytes[3].toInt() and 0xFF
            val body = bytes.copyOfRange(4, bytes.size)
            return when {
                body.isEmpty() -> CommandApdu(cla, ins, p1, p2)
                body.size == 1 -> CommandApdu(cla, ins, p1, p2, le = decodeLe(body[0]))
                else -> {
                    val lc = body[0].toInt() and 0xFF
                    when (body.size) {
                        1 + lc -> CommandApdu(cla, ins, p1, p2, body.copyOfRange(1, 1 + lc))
                        2 + lc -> CommandApdu(cla, ins, p1, p2, body.copyOfRange(1, 1 + lc), decodeLe(body.last()))
                        else -> throw ApduFormatException("Lc says $lc data bytes but the body has ${body.size} bytes")
                    }
                }
            }
        }

        private fun decodeLe(byte: Byte): Int = (byte.toInt() and 0xFF).let { if (it == 0) MAX_SHORT_LE else it }
    }
}
