package com.danyastry.nfcpay.core.emv.crypto

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class TripleDesTest {

    private val key = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()

    @Test
    fun `encrypts and decrypts one block with a double length key`() {
        val cipher = TripleDes.encrypt(key, ByteArray(8))

        assertThat(cipher.toHex()).isEqualTo("08D7B4FB629D0885")
        assertThat(TripleDes.decrypt(key, cipher)).isEqualTo(ByteArray(8))
    }

    @Test
    fun `the published check value of the classic test key`() {
        assertThat(KeyCheckValue.of(key)).isEqualTo("08D7B4")
    }

    @Test
    fun `rejects wrong key and block sizes`() {
        assertThrows(IllegalArgumentException::class.java) { TripleDes.encrypt(ByteArray(24), ByteArray(8)) }
        assertThrows(IllegalArgumentException::class.java) { TripleDes.encrypt(key, ByteArray(16)) }
    }

    @Test
    fun `sets odd parity on every byte`() {
        val adjusted = DesParity.adjust("0016F2F07C71E19E".hexToBytes())

        assertThat(adjusted.toHex()).isEqualTo("0116F2F17C70E09E")
        assertThat(DesParity.isOdd(adjusted)).isTrue()
        assertThat(DesParity.isOdd(key)).isTrue()
        assertThat(DesParity.isOdd("0000".hexToBytes())).isFalse()
    }
}
