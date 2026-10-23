package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class AflEntryTest {

    @Test
    fun `splits the AFL into files and record ranges`() {
        val entries = AflEntry.parse("080102001001030118010100".hexToBytes())

        assertThat(entries).containsExactly(
            AflEntry(sfi = 1, firstRecord = 1, lastRecord = 2, offlineAuthRecords = 0),
            AflEntry(sfi = 2, firstRecord = 1, lastRecord = 3, offlineAuthRecords = 1),
            AflEntry(sfi = 3, firstRecord = 1, lastRecord = 1, offlineAuthRecords = 0),
        ).inOrder()
        assertThat(entries[1].records.toList()).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `rejects broken entries`() {
        listOf("080102", "08000100", "08030100", "08010103", "00010100", "F8010100").forEach { hex ->
            val error = assertThrows(KernelException::class.java) { AflEntry.parse(hex.hexToBytes()) }
            assertThat(error.error).isEqualTo(TerminalError.BAD_AFL)
        }
    }
}
