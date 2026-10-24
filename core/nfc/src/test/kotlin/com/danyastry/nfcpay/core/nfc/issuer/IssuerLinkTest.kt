package com.danyastry.nfcpay.core.nfc.issuer

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class IssuerLinkTest {

    @Test
    fun `records every answer the issuer gives`() = runTest {
        val link = IssuerLink(DemoIssuer.create(), logLimit = 2)
        val card = VirtualCard(DemoCards.lab)
        val kernel = TerminalKernel(authorizer = link.authorizer)

        repeat(3) { kernel.run(LoopbackTransport(card), TransactionRequest(amount = 100, date = LocalDate.of(2026, 10, 24))) }

        assertThat(link.responses.value.map { it.atc }).containsExactly(2, 3).inOrder()
        assertThat(link.host.account(DemoCards.lab.pan, 1)!!.balance).isEqualTo(DemoIssuer.OPENING_BALANCE - 300)
    }
}
