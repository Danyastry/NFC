package com.danyastry.nfcpay.core.emv.pin

import com.danyastry.nfcpay.core.emv.crypto.Aes
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.xor
import kotlin.random.Random

object IsoPinBlock {

    const val MIN_PIN = 4
    const val MAX_PIN = 12
    private const val LEGACY_NIBBLES = 16
    private const val AES_NIBBLES = 32
    private const val HEX = "0123456789ABCDEF"

    fun format0(pin: String, pan: String): ByteArray =
        pinField(PinBlockFormat.FORMAT_0, pin) { "F" }.hexToBytes() xor legacyPanField(pan)

    fun format1(pin: String, random: Random = Random.Default): ByteArray =
        pinField(PinBlockFormat.FORMAT_1, pin) { HEX[random.nextInt(16)].toString() }.hexToBytes()

    fun format3(pin: String, pan: String, random: Random = Random.Default): ByteArray =
        pinField(PinBlockFormat.FORMAT_3, pin) { HEX[10 + random.nextInt(6)].toString() }.hexToBytes() xor legacyPanField(pan)

    fun decode(block: ByteArray, format: PinBlockFormat, pan: String? = null): String {
        if (block.size != LEGACY_NIBBLES / 2) throw PinBlockException("Legacy PIN blocks are 8 bytes, got ${block.size}")
        val field = when (format) {
            PinBlockFormat.FORMAT_0, PinBlockFormat.FORMAT_3 -> block xor legacyPanField(requireNotNull(pan) { "This format needs the PAN" })
            PinBlockFormat.FORMAT_1 -> block
            PinBlockFormat.FORMAT_4 -> throw PinBlockException("Format 4 is a 16-byte AES block, use decipherFormat4")
        }.toHex()
        val pin = readPin(field, format)
        val fill = field.substring(2 + pin.length)
        val fillValid = when (format) {
            PinBlockFormat.FORMAT_0 -> fill.all { it == 'F' }
            PinBlockFormat.FORMAT_3 -> fill.all { it in 'A'..'F' }
            else -> true
        }
        if (!fillValid) throw PinBlockException("Fill digits are wrong for ${format.name}")
        return pin
    }

    fun encipherFormat4(key: ByteArray, pin: String, pan: String, random: Random = Random.Default): ByteArray {
        val pinField = format4PinField(pin, random)
        return Aes.encrypt(key, Aes.encrypt(key, pinField) xor format4PanField(pan))
    }

    fun decipherFormat4(key: ByteArray, block: ByteArray, pan: String): String {
        if (block.size != AES_NIBBLES / 2) throw PinBlockException("Format 4 blocks are 16 bytes, got ${block.size}")
        val field = Aes.decrypt(key, Aes.decrypt(key, block) xor format4PanField(pan)).toHex()
        val pin = readPin(field, PinBlockFormat.FORMAT_4)
        if (!field.substring(2 + pin.length, LEGACY_NIBBLES).all { it == 'A' }) throw PinBlockException("Fill digits are wrong for FORMAT_4")
        return pin
    }

    internal fun format4PinField(pin: String, random: Random): ByteArray {
        val head = pinField(PinBlockFormat.FORMAT_4, pin) { "A" }
        val tail = (1..LEGACY_NIBBLES).joinToString("") { HEX[random.nextInt(16)].toString() }
        return (head + tail).hexToBytes()
    }

    internal fun format4PanField(pan: String): ByteArray {
        requirePan(pan)
        val digits = if (pan.length < 12) pan.padStart(12, '0') else pan
        return ((digits.length - 12).toString() + digits).padEnd(AES_NIBBLES, '0').hexToBytes()
    }

    internal fun legacyPanField(pan: String): ByteArray {
        requirePan(pan)
        return ("0000" + pan.dropLast(1).takeLast(12).padStart(12, '0')).hexToBytes()
    }

    private fun pinField(format: PinBlockFormat, pin: String, fill: () -> String): String {
        require(pin.length in MIN_PIN..MAX_PIN && pin.all(Char::isDigit)) { "PIN must be $MIN_PIN to $MAX_PIN digits" }
        val head = format.control.toString(16) + pin.length.toString(16) + pin
        return head + (1..LEGACY_NIBBLES - head.length).joinToString("") { fill() }
    }

    private fun readPin(field: String, format: PinBlockFormat): String {
        if (Character.digit(field[0], 16) != format.control) {
            throw PinBlockException("Control nibble ${field[0]} does not match ${format.name}")
        }
        val length = Character.digit(field[1], 16)
        if (length !in MIN_PIN..MAX_PIN) throw PinBlockException("PIN length $length is outside $MIN_PIN..$MAX_PIN")
        val pin = field.substring(2, 2 + length)
        if (!pin.all(Char::isDigit)) throw PinBlockException("PIN digits are not decimal")
        return pin
    }

    private fun requirePan(pan: String) {
        require(pan.length in 1..19 && pan.all(Char::isDigit)) { "PAN must be 1 to 19 digits" }
    }
}
