package com.danyastry.nfcpay.core.nfc.hce

import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.cardemulation.HostApduService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.terminal.TransactionOutcome
import com.danyastry.nfcpay.core.emv.terminal.TransactionRequest
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.core.nfc.tag.TagEmulationService
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PaymentCardServiceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun serviceIsRegisteredForHostCardEmulation() {
        val services = context.packageManager.queryIntentServices(
            Intent(HostApduService.SERVICE_INTERFACE).setPackage(context.packageName),
            PackageManager.GET_META_DATA,
        )

        assertThat(services.map { it.serviceInfo.name }).containsExactly(
            PaymentCardService::class.java.name,
            TagEmulationService::class.java.name,
        )
        val service = services.first { it.serviceInfo.name == PaymentCardService::class.java.name }.serviceInfo
        assertThat(service.name).isEqualTo(PaymentCardService::class.java.name)
        assertThat(service.permission).isEqualTo(android.Manifest.permission.BIND_NFC_SERVICE)
        assertThat(service.metaData.getInt(HostApduService.SERVICE_META_DATA)).isNotEqualTo(0)
    }

    @Test
    fun serviceAnswersAFullTransaction() = runTest {
        lateinit var service: PaymentCardService
        InstrumentationRegistry.getInstrumentation().runOnMainSync { service = PaymentCardService() }
        val before = SharedCard.host.atc.value
        val transport = CardTransport { command -> service.processCommandApdu(command, null) }

        val result = TerminalKernel().run(transport, TransactionRequest(amount = 250, date = LocalDate.now()))
        service.onDeactivated(HostApduService.DEACTIVATION_LINK_LOSS)

        val outcome = result.outcome as TransactionOutcome.Completed
        assertThat(outcome.decision).isEqualTo(CryptogramType.ARQC)
        assertThat(SharedCard.host.atc.value).isEqualTo(before + 1)
        assertThat(SharedCard.host.log.value.last().source).isEqualTo(ApduSource.NFC)
    }
}
