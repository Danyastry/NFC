package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.crypto.IccKeyDerivation
import com.danyastry.nfcpay.core.emv.crypto.LimitedUseKeyDerivation
import java.time.YearMonth

class TokenService(
    val vault: TokenVault,
    private val issuerMasterKey: ByteArray,
    val usesPerKey: Int = DEFAULT_USES_PER_KEY,
) {
    private val activeIndex = mutableMapOf<String, Int>()

    init {
        require(usesPerKey >= 1) { "Each key needs at least one use" }
    }

    @Synchronized
    fun provisionWallet(pan: String, panSequenceNumber: Int, requestorId: String, expiry: YearMonth): WalletProvisioning {
        val record = vault.provision(pan, panSequenceNumber, requestorId, TokenDomain.CONTACTLESS, expiry)
        return WalletProvisioning(record, issueKey(record.token, index = 1))
    }

    @Synchronized
    fun replenish(token: String): LimitedUseKey {
        val record = requireNotNull(vault.record(token)) { "Unknown token" }
        check(record.status == TokenStatus.ACTIVE) { "Only active tokens get new keys, this one is ${record.status}" }
        val next = (activeIndex[token] ?: 0) + 1
        check(next <= LimitedUseKeyDerivation.MAX_INDEX) { "Token has used all key indexes" }
        return issueKey(token, next)
    }

    @Synchronized
    fun activeKeyIndex(token: String): Int? = activeIndex[token]

    @Synchronized
    fun keyFor(token: String, index: Int): ByteArray? =
        if (activeIndex[token] == index) LimitedUseKeyDerivation.derive(tokenMasterKey(token), index) else null

    fun tokenMasterKey(token: String): ByteArray = IccKeyDerivation.optionA(issuerMasterKey, token, TOKEN_SEQUENCE_NUMBER)

    private fun issueKey(token: String, index: Int): LimitedUseKey {
        activeIndex[token] = index
        return LimitedUseKey(index, LimitedUseKeyDerivation.derive(tokenMasterKey(token), index), usesPerKey)
    }

    companion object {
        const val DEFAULT_USES_PER_KEY = 3
        const val TOKEN_SEQUENCE_NUMBER = 0
    }
}
