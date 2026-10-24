package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.util.Luhn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

class TokenVault(
    private val tokenBin: String = DEFAULT_TOKEN_BIN,
    private val binControllerId: String = DEFAULT_BIN_CONTROLLER_ID,
    private val random: Random = Random.Default,
) {
    private val records = linkedMapOf<String, TokenRecord>()
    private val references = mutableMapOf<String, String>()

    init {
        require(tokenBin.length == BIN_LENGTH && tokenBin.all(Char::isDigit)) { "Token BIN must be $BIN_LENGTH digits" }
        require(binControllerId.length == PAR_PREFIX_LENGTH && binControllerId.all(::isParChar)) {
            "BIN controller id must be $PAR_PREFIX_LENGTH upper case letters or digits"
        }
    }

    @Synchronized
    fun provision(pan: String, panSequenceNumber: Int, requestorId: String, domain: TokenDomain, expiry: YearMonth): TokenRecord {
        require(Luhn.isValid(pan)) { "PAN fails the Luhn check" }
        require(requestorId.length == REQUESTOR_ID_LENGTH && requestorId.all(Char::isDigit)) {
            "Token requestor id is $REQUESTOR_ID_LENGTH digits, got '$requestorId'"
        }
        val record = TokenRecord(
            token = newToken(pan),
            pan = pan,
            panSequenceNumber = panSequenceNumber,
            expiry = expiry,
            requestorId = requestorId,
            domain = domain,
            status = TokenStatus.ACTIVE,
            paymentAccountReference = references.getOrPut(pan, ::newReference),
        )
        records[record.token] = record
        return record
    }

    @Synchronized
    fun detokenize(token: String, domain: TokenDomain, date: LocalDate): DetokenizeResult {
        val record = records[token] ?: return DetokenizeResult.Refused(TokenRefusal.UNKNOWN_TOKEN)
        val refusal = when {
            record.status == TokenStatus.DELETED -> TokenRefusal.DELETED
            record.status == TokenStatus.SUSPENDED -> TokenRefusal.SUSPENDED
            record.domain != domain -> TokenRefusal.WRONG_DOMAIN
            record.expiry.isBefore(YearMonth.from(date)) -> TokenRefusal.EXPIRED
            else -> null
        }
        return refusal?.let { DetokenizeResult.Refused(it) } ?: DetokenizeResult.Found(record)
    }

    @Synchronized
    fun record(token: String): TokenRecord? = records[token]

    @Synchronized
    fun tokensFor(pan: String): List<TokenRecord> = records.values.filter { it.pan == pan }

    @Synchronized
    fun isToken(number: String): Boolean = number in records

    fun suspend(token: String): TokenRecord = move(token, from = TokenStatus.ACTIVE, to = TokenStatus.SUSPENDED)

    fun resume(token: String): TokenRecord = move(token, from = TokenStatus.SUSPENDED, to = TokenStatus.ACTIVE)

    @Synchronized
    fun delete(token: String): TokenRecord {
        val record = requireNotNull(records[token]) { "Unknown token" }
        return record.copy(status = TokenStatus.DELETED).also { records[token] = it }
    }

    @Synchronized
    private fun move(token: String, from: TokenStatus, to: TokenStatus): TokenRecord {
        val record = requireNotNull(records[token]) { "Unknown token" }
        check(record.status == from) { "Token is ${record.status}, expected $from" }
        return record.copy(status = to).also { records[token] = it }
    }

    private fun newToken(pan: String): String {
        while (true) {
            val body = tokenBin + (1..TOKEN_LENGTH - BIN_LENGTH - 1).map { random.nextInt(10) }.joinToString("")
            val token = body + Luhn.checkDigit(body)
            if (token != pan && token !in records) return token
        }
    }

    private fun newReference(): String {
        while (true) {
            val reference = binControllerId + (1..PAR_LENGTH - PAR_PREFIX_LENGTH).map { PAR_ALPHABET[random.nextInt(PAR_ALPHABET.length)] }.joinToString("")
            if (reference !in references.values) return reference
        }
    }

    companion object {
        const val DEFAULT_TOKEN_BIN = "999950"
        const val DEFAULT_BIN_CONTROLLER_ID = "Q999"
        const val TOKEN_LENGTH = 16
        const val BIN_LENGTH = 6
        const val REQUESTOR_ID_LENGTH = 11
        const val PAR_LENGTH = 29
        const val PAR_PREFIX_LENGTH = 4
        private const val PAR_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        private fun isParChar(char: Char): Boolean = char in PAR_ALPHABET
    }
}
