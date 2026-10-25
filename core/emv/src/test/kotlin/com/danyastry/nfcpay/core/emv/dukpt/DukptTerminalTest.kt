package com.danyastry.nfcpay.core.emv.dukpt

import com.danyastry.nfcpay.core.emv.pin.IsoPinBlock
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DukptTerminalTest {

    private val bdk = "FEDCBA9876543210F1F1F1F1F1F1F1F1".hexToBytes()
    private val initialKeyId = "1234567890123456".hexToBytes()
    private val initialKey = AesDukpt.initialKey(bdk, initialKeyId)

    @Test
    fun `the device keeps future keys instead of the initial key and still matches the host`() {
        val terminal = DukptTerminal(initialKey, initialKeyId)

        repeat(1_000) {
            val transaction = terminal.nextTransaction()

            assertThat(transaction.key).isEqualTo(AesDukpt.transactionKey(initialKey, transaction.ksn))
        }
        assertThat(terminal.storedKeys).isAtMost(32)
        assertThat(terminal.nextCounter).isEqualTo(1_001)
    }

    @Test
    fun `skips counters with too many bits set`() {
        val terminal = DukptTerminal(initialKey, initialKeyId)
        val keys = mutableMapOf<Long, ByteArray>()

        while (terminal.nextCounter <= 0x20001L) {
            val transaction = terminal.nextTransaction()
            keys[transaction.ksn.counter] = transaction.key
        }

        assertThat(keys.keys).doesNotContain(0x1FFFFL)
        assertThat(keys.keys.all { java.lang.Long.bitCount(it) <= 16 }).isTrue()
        assertThat(keys.getValue(0x1FFFEL).toHex()).isEqualTo("E21E8C8D347F8561A2BE752DAA85A111")
        assertThat(keys.getValue(0x20000L).toHex()).isEqualTo("F7AE9025468A25D37B7249CFFED224C8")
        assertThat(keys.getValue(0x20001L).toHex()).isEqualTo("692C8EA4013645135B36497131DF9D2C")
        assertThat(terminal.storedKeys).isAtMost(32)
    }

    @Test
    fun `the host decrypts every PIN with only the base key and the KSN`() {
        val terminal = DukptTerminal(initialKey, initialKeyId)

        repeat(5) {
            val encrypted = terminal.encryptPin("4321", "9999001234567891")
            val pinKey = AesDukpt.hostWorkingKey(bdk, encrypted.ksn, DukptKeyUsage.PIN_ENCRYPTION)

            assertThat(IsoPinBlock.decipherFormat4(pinKey, encrypted.block, "9999001234567891")).isEqualTo("4321")
        }
    }

    @Test
    fun `the same PIN never produces the same block twice`() {
        val terminal = DukptTerminal(initialKey, initialKeyId)

        val blocks = (1..20).map { terminal.encryptPin("1234", "9999001234567891").block.toList() }

        assertThat(blocks).containsNoDuplicates()
    }
}
