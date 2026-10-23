package com.danyastry.nfcpay.core.nfc.hce

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class CardHostTest {

    private var now = 1_000L
    private val host = CardHost(DemoCards.lab, clock = { now++ }, logLimit = 8)
    private val kernel = TerminalKernel()

    @Test
    fun `logs every command with its source and tracks the counter`() = runTest {
        kernel.run(LoopbackTransport(host.handler(ApduSource.SIMULATED)), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 23)))

        assertThat(host.atc.value).isEqualTo(1)
        assertThat(host.log.value).hasSize(6)
        assertThat(host.log.value.map { it.source }.toSet()).containsExactly(ApduSource.SIMULATED)
        assertThat(host.log.value.map { it.timeMillis }).isInStrictOrder()
    }

    @Test
    fun `keeps only the newest entries`() = runTest {
        repeat(2) {
            kernel.run(LoopbackTransport(host.handler(ApduSource.NFC)), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 23)))
        }

        assertThat(host.log.value).hasSize(8)
        assertThat(host.atc.value).isEqualTo(2)
    }

    @Test
    fun `resets the counter and clears the log`() = runTest {
        val result = kernel.run(LoopbackTransport(host.handler(ApduSource.SIMULATED)), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 23)))
        assertThat(result.outcome).isInstanceOf(TransactionOutcome.Completed::class.java)

        host.resetCounter()
        host.clearLog()

        assertThat(host.atc.value).isEqualTo(0)
        assertThat(host.log.value).isEmpty()
    }
}
