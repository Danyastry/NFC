package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.Instruction
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.apdu.StatusWord
import com.danyastry.nfcpay.core.emv.card.CardCryptogram
import com.danyastry.nfcpay.core.emv.card.CardProfile
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.tlv.valueOf
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.core.emv.transport.LoopbackTransport
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class TerminalKernelTest {

    private val date = LocalDate.of(2026, 10, 23)
    private val kernel = TerminalKernel(random = Random(7))

    @Test
    fun `runs the full contactless flow against the virtual card`() = runTest {
        val card = VirtualCard(DemoCards.lab)

        val result = kernel.run(LoopbackTransport(card), TransactionRequest(amount = 1234, date = date))

        assertThat(result.exchanges.map { it.label }).containsExactly(
            "SELECT PPSE",
            "SELECT F04E464350415901",
            "GET PROCESSING OPTIONS",
            "READ RECORD · SFI 1 · record 1",
            "READ RECORD · SFI 1 · record 2",
            "GENERATE AC · ARQC",
        ).inOrder()
        assertThat(result.exchanges.all { it.response.isSuccess }).isTrue()
        val outcome = result.outcome as TransactionOutcome.Completed
        assertThat(outcome.decision).isEqualTo(CryptogramType.ARQC)
        assertThat(outcome.card).isEqualTo(
            CardSummary(
                pan = "9999001234567891",
                expiry = YearMonth.of(2031, 12),
                label = "NFC PAY LAB",
                aid = "F04E464350415901",
                scheme = PaymentScheme.LAB,
                panSequenceNumber = 1,
            ),
        )
        assertThat(outcome.cryptogram.atc).isEqualTo(1)
        assertThat(outcome.cryptogram.fromProcessingOptions).isFalse()
        assertThat(outcome.tvr.bits).containsExactly(
            TvrBit.OFFLINE_DATA_AUTHENTICATION_NOT_PERFORMED,
            TvrBit.FLOOR_LIMIT_EXCEEDED,
        )
        assertThat(outcome.cardholderVerificationRequired).isFalse()
    }

    @Test
    fun `sends the issuer everything it needs to check the cryptogram`() = runTest {
        val result = kernel.run(LoopbackTransport(VirtualCard(DemoCards.lab)), TransactionRequest(amount = 1234, date = date))

        val field55 = BerTlv.parse((result.outcome as TransactionOutcome.Completed).field55.hexToBytes())
        val cdolData = listOf(
            EmvTags.AMOUNT_AUTHORISED, EmvTags.AMOUNT_OTHER, EmvTags.TERMINAL_COUNTRY, EmvTags.TVR,
            EmvTags.TRANSACTION_CURRENCY, EmvTags.TRANSACTION_DATE, EmvTags.TRANSACTION_TYPE, EmvTags.UNPREDICTABLE_NUMBER,
        ).fold(ByteArray(0)) { acc, tag -> acc + field55.valueOf(tag)!! }
        val atc = field55.valueOf(EmvTags.ATC)!!.toUnsignedInt()
        val recomputed = CardCryptogram.compute(
            key = CardCryptogram.key(DemoCards.lab.masterKey, CryptogramVersion.SESSION_KEY, atc),
            cdolData = cdolData,
            aip = field55.valueOf(EmvTags.AIP)!!,
            atc = atc,
            cvr = CardCryptogram.cvr(CryptogramType.ARQC),
        )
        assertThat(field55.valueOf(EmvTags.APPLICATION_CRYPTOGRAM)).isEqualTo(recomputed)
        assertThat(field55.valueOf(EmvTags.TRANSACTION_DATE)!!.toHex()).isEqualTo("261023")
        assertThat(field55.valueOf(EmvTags.AMOUNT_AUTHORISED)!!.toHex()).isEqualTo("000000001234")
        assertThat(field55.valueOf(EmvTags.TVR)!!.toHex()).isEqualTo("8000008000")
        assertThat(field55.map { it.tag }).isInOrder(compareBy<Tag> { Field55.TAGS.indexOf(it) })
    }

    @Test
    fun `uses a fresh unpredictable number for every tap`() = runTest {
        val card = VirtualCard(DemoCards.lab)
        val first = kernel.run(LoopbackTransport(card), TransactionRequest(amount = 100, date = date)).outcome as TransactionOutcome.Completed
        val second = kernel.run(LoopbackTransport(card), TransactionRequest(amount = 100, date = date)).outcome as TransactionOutcome.Completed

        assertThat(second.cryptogram.atc).isEqualTo(first.cryptogram.atc + 1)
        assertThat(second.cryptogram.value).isNotEqualTo(first.cryptogram.value)
    }

    @Test
    fun `flags an expired card and a card that is not yet valid`() = runTest {
        val expired = profile(expiry = YearMonth.of(2025, 1), effective = YearMonth.of(2023, 1))
        val future = profile(expiry = YearMonth.of(2030, 1), effective = YearMonth.of(2027, 1))

        val expiredTvr = (kernel.run(LoopbackTransport(VirtualCard(expired)), TransactionRequest(amount = 1, date = date)).outcome as TransactionOutcome.Completed).tvr
        val futureTvr = (kernel.run(LoopbackTransport(VirtualCard(future)), TransactionRequest(amount = 1, date = date)).outcome as TransactionOutcome.Completed).tvr

        assertThat(expiredTvr.bits).contains(TvrBit.EXPIRED_APPLICATION)
        assertThat(futureTvr.bits).contains(TvrBit.APPLICATION_NOT_EFFECTIVE)
    }

    @Test
    fun `asks for cardholder verification above the contactless limit`() = runTest {
        val result = kernel.run(LoopbackTransport(VirtualCard(DemoCards.lab)), TransactionRequest(amount = 5_001, date = date))

        assertThat((result.outcome as TransactionOutcome.Completed).cardholderVerificationRequired).isTrue()
    }

    @Test
    fun `selects known AIDs directly when the card has no PPSE`() = runTest {
        val card = VirtualCard(DemoCards.lab)
        val noPpse = CardTransport { command ->
            if (command.contentEquals(EmvCommands.select(EmvCommands.PPSE).encode())) {
                ResponseApdu.status(StatusWord.FILE_NOT_FOUND).encode()
            } else {
                card.process(command)
            }
        }

        val result = kernel.run(noPpse, TransactionRequest(amount = 1, date = date))

        assertThat(result.exchanges.take(2).map { it.label }).containsExactly("SELECT PPSE", "SELECT F04E464350415901").inOrder()
        assertThat(result.outcome).isInstanceOf(TransactionOutcome.Completed::class.java)
    }

    @Test
    fun `fails cleanly when no application matches`() = runTest {
        val result = kernel.run({ ResponseApdu.status(StatusWord.FILE_NOT_FOUND).encode() }, TransactionRequest(amount = 1, date = date))

        assertThat(result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.NO_SUPPORTED_APPLICATION))
        assertThat(result.exchanges).hasSize(1 + PaymentScheme.entries.size)
    }

    @Test
    fun `keeps the log when the card refuses processing options`() = runTest {
        val card = VirtualCard(DemoCards.lab)
        val refusing = CardTransport { command ->
            if (CommandApdu.parse(command).instruction == Instruction.GET_PROCESSING_OPTIONS) {
                ResponseApdu.status(StatusWord.CONDITIONS_NOT_SATISFIED).encode()
            } else {
                card.process(command)
            }
        }

        val result = kernel.run(refusing, TransactionRequest(amount = 1, date = date))

        assertThat(result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.PROCESSING_OPTIONS_REFUSED, StatusWord.CONDITIONS_NOT_SATISFIED))
        assertThat(result.exchanges.last().label).isEqualTo("GET PROCESSING OPTIONS")
    }

    @Test
    fun `reports a card that leaves the field`() = runTest {
        val card = VirtualCard(DemoCards.lab)
        var commands = 0
        val leaving = CardTransport { command -> if (++commands > 3) throw IOException("Tag was lost") else card.process(command) }

        val result = kernel.run(leaving, TransactionRequest(amount = 1, date = date))

        assertThat(result.outcome).isEqualTo(TransactionOutcome.Failed(TerminalError.CARD_LOST))
        assertThat(result.exchanges).hasSize(3)
    }

    @Test
    fun `reads format 1 responses`() = runTest {
        val card = FormatOneCard()

        val result = kernel.run(card, TransactionRequest(amount = 500, date = date))

        val outcome = result.outcome as TransactionOutcome.Completed
        assertThat(outcome.card.pan).isEqualTo("4111111111111111")
        assertThat(outcome.card.expiry).isEqualTo(YearMonth.of(2028, 6))
        assertThat(outcome.card.scheme).isEqualTo(PaymentScheme.VISA)
        assertThat(outcome.cryptogram).isEqualTo(
            Cryptogram(CryptogramType.ARQC, "1122334455667788", atc = 0x2A, issuerApplicationData = "06010A03A00000", fromProcessingOptions = false),
        )
    }

    @Test
    fun `takes the cryptogram from processing options when the card sends it there`() = runTest {
        val card = QuickCard()

        val result = kernel.run(card, TransactionRequest(amount = 500, date = date))

        assertThat(result.exchanges.map { it.label }).doesNotContain("GENERATE AC · ARQC")
        val outcome = result.outcome as TransactionOutcome.Completed
        assertThat(outcome.cryptogram.fromProcessingOptions).isTrue()
        assertThat(outcome.cryptogram.value).isEqualTo("8877665544332211")
        assertThat(outcome.card.pan).isEqualTo("4111111111111111")
    }

    private fun profile(expiry: YearMonth, effective: YearMonth) = CardProfile(
        aid = DemoCards.lab.aid,
        label = "TEST",
        pan = DemoCards.lab.pan,
        expiry = expiry,
        effective = effective,
        cardholderName = "TEST",
        panSequenceNumber = 0,
        issuerCountryCode = 840,
        currencyCode = 840,
        masterKey = DemoCards.lab.masterKey,
    )

    private class FormatOneCard : CardTransport {
        override suspend fun transceive(command: ByteArray): ByteArray {
            val apdu = CommandApdu.parse(command)
            val data = when (apdu.instruction) {
                Instruction.SELECT -> if (apdu.data.toHex() == "A0000000031010") "6F0A8407A0000000031010A500" else null
                Instruction.GET_PROCESSING_OPTIONS -> "800618800801010 0".replace(" ", "")
                Instruction.READ_RECORD -> Tlv.constructed(
                    EmvTags.RECORD_TEMPLATE,
                    Tlv.primitive(EmvTags.TRACK_2_EQUIVALENT, "4111111111111111D280620100000000000F".hexToBytes()),
                    Tlv.primitive(EmvTags.CDOL1, "9F02069F3704".hexToBytes()),
                ).encode().toHex()
                Instruction.GENERATE_AC -> "8012" + "80" + "002A" + "1122334455667788" + "06010A03A00000"
                else -> null
            }
            return (data?.let { ResponseApdu.success(it.hexToBytes()) } ?: ResponseApdu.status(StatusWord.FILE_NOT_FOUND)).encode()
        }
    }

    private class QuickCard : CardTransport {
        override suspend fun transceive(command: ByteArray): ByteArray {
            val apdu = CommandApdu.parse(command)
            val data = when (apdu.instruction) {
                Instruction.SELECT -> if (apdu.data.toHex() == "A0000000031010") "6F118407A0000000031010A5069F38039F6604" else null
                Instruction.GET_PROCESSING_OPTIONS -> Tlv.constructed(
                    EmvTags.RESPONSE_FORMAT_2,
                    Tlv.primitive(EmvTags.AIP, "2000".hexToBytes()),
                    Tlv.primitive(EmvTags.TRACK_2_EQUIVALENT, "4111111111111111D280620100000000000F".hexToBytes()),
                    Tlv.primitive(EmvTags.CRYPTOGRAM_INFORMATION_DATA, "80".hexToBytes()),
                    Tlv.primitive(EmvTags.ATC, "0005".hexToBytes()),
                    Tlv.primitive(EmvTags.APPLICATION_CRYPTOGRAM, "8877665544332211".hexToBytes()),
                ).encode().toHex()
                else -> null
            }
            return (data?.let { ResponseApdu.success(it.hexToBytes()) } ?: ResponseApdu.status(StatusWord.FILE_NOT_FOUND)).encode()
        }
    }
}
