package com.danyastry.nfcpay.core.ndef.message

class NdefRecord(
    val tnf: Tnf,
    val type: ByteArray = ByteArray(0),
    val payload: ByteArray = ByteArray(0),
    val id: ByteArray = ByteArray(0),
) {
    init {
        require(type.size <= 0xFF) { "Record types are at most 255 bytes" }
        require(id.size <= 0xFF) { "Record ids are at most 255 bytes" }
        if (tnf == Tnf.EMPTY) require(type.isEmpty() && payload.isEmpty() && id.isEmpty()) { "Empty records carry nothing" }
    }

    val typeName: String
        get() = type.toString(Charsets.US_ASCII)

    fun isType(expected: Tnf, name: String): Boolean = tnf == expected && typeName == name

    override fun equals(other: Any?): Boolean =
        other is NdefRecord && tnf == other.tnf && type.contentEquals(other.type) &&
            payload.contentEquals(other.payload) && id.contentEquals(other.id)

    override fun hashCode(): Int = 31 * (31 * tnf.hashCode() + type.contentHashCode()) + payload.contentHashCode()

    override fun toString(): String = "NdefRecord(${tnf.name}, '$typeName', ${payload.size} bytes)"
}
