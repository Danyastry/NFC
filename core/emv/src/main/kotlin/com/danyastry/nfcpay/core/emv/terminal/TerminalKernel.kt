package com.danyastry.nfcpay.core.emv.terminal

import com.danyastry.nfcpay.core.emv.apdu.ApduFormatException
import com.danyastry.nfcpay.core.emv.apdu.CommandApdu
import com.danyastry.nfcpay.core.emv.apdu.EmvCommands
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.card.CryptogramType
import com.danyastry.nfcpay.core.emv.dol.Dol
import com.danyastry.nfcpay.core.emv.tags.EmvTags
import com.danyastry.nfcpay.core.emv.tlv.BerTlv
import com.danyastry.nfcpay.core.emv.tlv.Tag
import com.danyastry.nfcpay.core.emv.tlv.Tlv
import com.danyastry.nfcpay.core.emv.tlv.TlvFormatException
import com.danyastry.nfcpay.core.emv.tlv.findTag
import com.danyastry.nfcpay.core.emv.tlv.flatten
import com.danyastry.nfcpay.core.emv.tlv.valueOf
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import com.danyastry.nfcpay.core.emv.util.Bcd
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.emv.util.toUnsignedInt
import java.io.IOException
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class TerminalKernel(
    private val config: TerminalConfig = TerminalConfig(),
    private val random: Random = Random.Default,
    private val authorizer: OnlineAuthorizer? = null,
    private val pinPad: PinPad? = null,
) {

    suspend fun run(transport: CardTransport, request: TransactionRequest): TransactionResult {
        val session = Session(transport)
        val outcome = try {
            process(session, request)
        } catch (failure: KernelException) {
            TransactionOutcome.Failed(failure.error, failure.sw)
        } catch (_: IOException) {
            TransactionOutcome.Failed(TerminalError.CARD_LOST)
        } catch (_: TlvFormatException) {
            TransactionOutcome.Failed(TerminalError.MALFORMED_RESPONSE)
        } catch (_: ApduFormatException) {
            TransactionOutcome.Failed(TerminalError.MALFORMED_RESPONSE)
        }
        return TransactionResult(session.exchanges.toList(), outcome)
    }

    private suspend fun process(session: Session, request: TransactionRequest): TransactionOutcome {
        val (candidate, fci) = selectApplication(session)
        val terminalData = terminalData(request).toMutableMap()

        val pdol = fci.valueOf(EmvTags.PDOL)?.let(Dol::parse).orEmpty()
        val gpo = session.exchange("GET PROCESSING OPTIONS", EmvCommands.getProcessingOptions(Dol.build(pdol, terminalData::get)))
        if (!gpo.isSuccess) throw KernelException(TerminalError.PROCESSING_OPTIONS_REFUSED, gpo.sw)
        val (aip, afl, gpoData) = parseProcessingOptions(gpo.data)

        val cardData = fci.flatten() + gpoData + readRecords(session, afl)
        val cardholderVerificationRequired = request.amount > config.cvmLimit
        val onlinePin = if (cardholderVerificationRequired && pinPad != null && authorizer != null) {
            val pan = CardDataReader.pan(cardData) ?: throw KernelException(TerminalError.MISSING_PAN)
            pinPad.capture(pan, request.amount) ?: throw KernelException(TerminalError.PIN_ENTRY_CANCELLED)
        } else {
            null
        }
        val tvr = riskManagement(cardData, request).let { if (onlinePin != null) it + TvrBit.ONLINE_PIN_ENTERED else it }
        terminalData[EmvTags.TVR] = tvr.encode()
        terminalData[EmvTags.CVM_RESULTS] = if (onlinePin != null) ONLINE_PIN_CVM else NO_CVM

        val cryptogramData = if (gpoData.findTag(EmvTags.APPLICATION_CRYPTOGRAM) != null) {
            gpoData
        } else {
            val cdol1 = cardData.valueOf(EmvTags.CDOL1)?.let(Dol::parse) ?: throw KernelException(TerminalError.MISSING_CDOL1)
            val command = EmvCommands.generateAc(CryptogramType.ARQC, Dol.build(cdol1, terminalData::get))
            val response = session.exchange("GENERATE AC - ARQC", command)
            if (!response.isSuccess) throw KernelException(TerminalError.GENERATE_AC_FAILED, response.sw)
            parseGenerateAc(response.data)
        }
        val cryptogram = cryptogram(cryptogramData, fromProcessingOptions = cryptogramData === gpoData)

        val allData = cardData + cryptogramData
        val pan = CardDataReader.pan(allData) ?: throw KernelException(TerminalError.MISSING_PAN)
        val card = CardSummary(
            pan = pan,
            expiry = CardDataReader.expiry(allData),
            label = candidate.label ?: CardDataReader.text(allData, EmvTags.APPLICATION_LABEL) ?: candidate.aid.toHex(),
            aid = candidate.aid.toHex(),
            scheme = PaymentScheme.of(candidate.aid),
            panSequenceNumber = CardDataReader.panSequenceNumber(allData),
        )
        val field55Values = buildMap {
            putAll(terminalData)
            put(EmvTags.AIP, aip)
            put(EmvTags.DF_NAME, candidate.aid)
            allData.valueOf(EmvTags.PAN_SEQUENCE_NUMBER)?.let { put(EmvTags.PAN_SEQUENCE_NUMBER, it) }
            listOf(EmvTags.APPLICATION_CRYPTOGRAM, EmvTags.CRYPTOGRAM_INFORMATION_DATA, EmvTags.ISSUER_APPLICATION_DATA, EmvTags.ATC)
                .forEach { tag -> cryptogramData.valueOf(tag)?.let { put(tag, it) } }
        }
        val field55 = Field55.build(field55Values)
        val online = authorizer?.takeIf { cryptogram.type == CryptogramType.ARQC }?.let {
            goOnline(session, it, OnlineRequest(card, field55, request.date, onlinePin), cardData, terminalData)
        }
        return TransactionOutcome.Completed(
            card = card,
            cryptogram = cryptogram,
            tvr = tvr,
            cardholderVerificationRequired = cardholderVerificationRequired,
            field55 = field55.toHex(),
            online = online,
        )
    }

    private suspend fun goOnline(
        session: Session,
        authorizer: OnlineAuthorizer,
        request: OnlineRequest,
        cardData: List<Tlv>,
        terminalData: MutableMap<Tag, ByteArray>,
    ): OnlineResult {
        val response = try {
            authorizer.authorize(request)
        } catch (_: IOException) {
            null
        }
        val responseCode = response?.responseCode ?: OnlineResponse.UNABLE_TO_GO_ONLINE
        val cdol2 = cardData.valueOf(EmvTags.CDOL2)?.let(Dol::parse)
            ?: return OnlineResult(responseCode, reachedIssuer = response != null, finalCryptogram = null)
        terminalData[EmvTags.AUTHORISATION_RESPONSE_CODE] = responseCode.toByteArray(Charsets.US_ASCII)
        response?.issuerAuthenticationData?.let { terminalData[EmvTags.ISSUER_AUTHENTICATION_DATA] = it }
        val requested = if (response?.isApproved == true) CryptogramType.TC else CryptogramType.AAC
        val reply = session.exchange("GENERATE AC - ${requested.label}", EmvCommands.generateAc(requested, Dol.build(cdol2, terminalData::get)))
        if (!reply.isSuccess) throw KernelException(TerminalError.GENERATE_AC_FAILED, reply.sw)
        val final = cryptogram(parseGenerateAc(reply.data), fromProcessingOptions = false)
        return OnlineResult(responseCode, reachedIssuer = response != null, finalCryptogram = final)
    }

    private suspend fun selectApplication(session: Session): Pair<ApplicationCandidate, List<Tlv>> {
        val ppse = session.exchange("SELECT PPSE", EmvCommands.select(EmvCommands.PPSE))
        val candidates = if (ppse.isSuccess) directory(ppse.data) else emptyList()
        val ordered = candidates.ifEmpty {
            config.schemes.map { ApplicationCandidate(it.defaultAid.hexToBytes(), null, Int.MAX_VALUE) }
        }
        for (candidate in ordered) {
            val response = session.exchange("SELECT ${candidate.aid.toHex()}", EmvCommands.select(candidate.aid))
            if (response.isSuccess) return candidate to BerTlv.parse(response.data)
        }
        throw KernelException(TerminalError.NO_SUPPORTED_APPLICATION)
    }

    private fun directory(data: ByteArray): List<ApplicationCandidate> {
        val discretionary = BerTlv.parse(data).findTag(EmvTags.FCI_ISSUER_DISCRETIONARY) ?: return emptyList()
        return discretionary.children
            .filter { it.tag == EmvTags.APPLICATION_TEMPLATE }
            .mapNotNull { entry ->
                val aid = entry.children.valueOf(EmvTags.AID) ?: return@mapNotNull null
                if (PaymentScheme.of(aid) !in config.schemes) return@mapNotNull null
                val priority = entry.children.valueOf(EmvTags.APPLICATION_PRIORITY)?.firstOrNull()?.toInt()?.and(0x0F)
                ApplicationCandidate(
                    aid = aid,
                    label = entry.children.valueOf(EmvTags.APPLICATION_LABEL)?.toString(Charsets.US_ASCII)?.trim(),
                    priority = priority?.takeIf { it != 0 } ?: Int.MAX_VALUE,
                )
            }
            .sortedBy { it.priority }
    }

    private fun parseProcessingOptions(data: ByteArray): Triple<ByteArray, ByteArray, List<Tlv>> {
        val tlvs = BerTlv.parse(data)
        tlvs.findTag(EmvTags.RESPONSE_FORMAT_1)?.takeIf { tlvs.findTag(EmvTags.RESPONSE_FORMAT_2) == null }?.let { format1 ->
            if (format1.value.size < 6) throw KernelException(TerminalError.MISSING_AIP_OR_AFL)
            val aip = format1.value.copyOfRange(0, 2)
            val afl = format1.value.copyOfRange(2, format1.value.size)
            return Triple(aip, afl, listOf(Tlv.primitive(EmvTags.AIP, aip), Tlv.primitive(EmvTags.AFL, afl)))
        }
        val flat = tlvs.flatten()
        val aip = flat.valueOf(EmvTags.AIP) ?: throw KernelException(TerminalError.MISSING_AIP_OR_AFL)
        val afl = flat.valueOf(EmvTags.AFL) ?: ByteArray(0)
        return Triple(aip, afl, flat)
    }

    private suspend fun readRecords(session: Session, afl: ByteArray): List<Tlv> {
        if (afl.isEmpty()) return emptyList()
        return AflEntry.parse(afl).flatMap { entry ->
            entry.records.flatMap { number ->
                val response = session.exchange("READ RECORD - SFI ${entry.sfi} - record $number", EmvCommands.readRecord(entry.sfi, number))
                if (!response.isSuccess) throw KernelException(TerminalError.READ_RECORD_FAILED, response.sw)
                BerTlv.parse(response.data).flatten()
            }
        }
    }

    private fun riskManagement(cardData: List<Tlv>, request: TransactionRequest): Tvr {
        var tvr = Tvr() + TvrBit.OFFLINE_DATA_AUTHENTICATION_NOT_PERFORMED
        val month = YearMonth.from(request.date)
        CardDataReader.expiry(cardData)?.let { if (it.isBefore(month)) tvr += TvrBit.EXPIRED_APPLICATION }
        CardDataReader.effective(cardData)?.let { if (it.isAfter(month)) tvr += TvrBit.APPLICATION_NOT_EFFECTIVE }
        if (request.amount > config.floorLimit) tvr += TvrBit.FLOOR_LIMIT_EXCEEDED
        return tvr
    }

    private fun parseGenerateAc(data: ByteArray): List<Tlv> {
        val tlvs = BerTlv.parse(data)
        val format1 = tlvs.findTag(EmvTags.RESPONSE_FORMAT_1)
        if (format1 == null || tlvs.findTag(EmvTags.RESPONSE_FORMAT_2) != null) return tlvs.flatten()
        val value = format1.value
        if (value.size < 11) throw KernelException(TerminalError.MISSING_CRYPTOGRAM)
        return listOf(
            Tlv.primitive(EmvTags.CRYPTOGRAM_INFORMATION_DATA, value.copyOfRange(0, 1)),
            Tlv.primitive(EmvTags.ATC, value.copyOfRange(1, 3)),
            Tlv.primitive(EmvTags.APPLICATION_CRYPTOGRAM, value.copyOfRange(3, 11)),
            Tlv.primitive(EmvTags.ISSUER_APPLICATION_DATA, value.copyOfRange(11, value.size)),
        )
    }

    private fun cryptogram(data: List<Tlv>, fromProcessingOptions: Boolean): Cryptogram {
        val value = data.valueOf(EmvTags.APPLICATION_CRYPTOGRAM) ?: throw KernelException(TerminalError.MISSING_CRYPTOGRAM)
        val cid = data.valueOf(EmvTags.CRYPTOGRAM_INFORMATION_DATA)?.firstOrNull()?.toInt()?.and(0xFF)
        val type = cid?.let(CryptogramType::fromBits) ?: CryptogramType.ARQC
        return Cryptogram(
            type = type,
            value = value.toHex(),
            atc = data.valueOf(EmvTags.ATC)?.toUnsignedInt() ?: 0,
            issuerApplicationData = data.valueOf(EmvTags.ISSUER_APPLICATION_DATA)?.toHex().orEmpty(),
            fromProcessingOptions = fromProcessingOptions,
        )
    }

    private fun terminalData(request: TransactionRequest): Map<Tag, ByteArray> = mapOf(
        EmvTags.TTQ to config.transactionQualifiers,
        EmvTags.AMOUNT_AUTHORISED to Bcd.encode(request.amount, 6),
        EmvTags.AMOUNT_OTHER to Bcd.encode(request.amountOther, 6),
        EmvTags.TERMINAL_COUNTRY to Bcd.encode(config.countryCode.toLong(), 2),
        EmvTags.TRANSACTION_CURRENCY to Bcd.encode(config.currencyCode.toLong(), 2),
        EmvTags.TRANSACTION_DATE to Bcd.encodeDigits(request.date.format(DATE_FORMAT), 3),
        EmvTags.TRANSACTION_TYPE to Bcd.encode(request.type.toLong(), 1),
        EmvTags.UNPREDICTABLE_NUMBER to random.nextBytes(4),
        EmvTags.TERMINAL_TYPE to Bcd.encode(config.terminalType.toLong(), 1),
        EmvTags.TERMINAL_CAPABILITIES to config.capabilities,
        EmvTags.ADDITIONAL_TERMINAL_CAPABILITIES to config.additionalCapabilities,
        EmvTags.MERCHANT_NAME to config.merchantName.toByteArray(Charsets.US_ASCII),
        EmvTags.TVR to Tvr().encode(),
    )

    private class Session(private val transport: CardTransport) {
        val exchanges = mutableListOf<Exchange>()

        suspend fun exchange(label: String, command: CommandApdu): ResponseApdu {
            var response = send(label, command)
            if (response.sw ushr 8 == WRONG_LE) {
                val le = expectedLength(response.sw)
                response = send("$label - retry with Le %02X".format(le and 0xFF), CommandApdu(command.cla, command.ins, command.p1, command.p2, command.data, le))
            }
            var data = response.data
            var fetches = 0
            while (response.sw ushr 8 == MORE_DATA) {
                if (++fetches > MAX_GET_RESPONSE) throw KernelException(TerminalError.MALFORMED_RESPONSE, response.sw)
                response = send("GET RESPONSE", CommandApdu(0x00, GET_RESPONSE, 0x00, 0x00, le = expectedLength(response.sw)))
                data += response.data
            }
            return ResponseApdu(data, response.sw)
        }

        private suspend fun send(label: String, command: CommandApdu): ResponseApdu {
            val response = ResponseApdu.parse(transport.transceive(command.encode()))
            exchanges += Exchange(label, command, response)
            return response
        }

        private fun expectedLength(sw: Int): Int = (sw and 0xFF).let { if (it == 0) CommandApdu.MAX_SHORT_LE else it }
    }

    internal companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd")
        const val MORE_DATA = 0x61
        const val WRONG_LE = 0x6C
        const val GET_RESPONSE = 0xC0
        const val MAX_GET_RESPONSE = 16
        val ONLINE_PIN_CVM: ByteArray = "420300".hexToBytes()
        val NO_CVM: ByteArray = "3F0000".hexToBytes()
    }
}
