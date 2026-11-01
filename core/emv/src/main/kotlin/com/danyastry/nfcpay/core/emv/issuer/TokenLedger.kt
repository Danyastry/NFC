package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.issuer.IssuerAccount.Companion.NO_TRANSACTIONS

class TokenLedger {

    private val counters = mutableMapOf<String, Int>()
    private val keyUses = mutableMapOf<String, Int>()

    fun lastAtc(token: String): Int = counters[token] ?: NO_TRANSACTIONS

    fun uses(token: String, keyIndex: Int): Int = keyUses[useKey(token, keyIndex)] ?: 0

    fun record(token: String, atc: Int, keyIndex: Int) {
        counters[token] = atc
        keyUses[useKey(token, keyIndex)] = uses(token, keyIndex) + 1
    }

    fun clear() {
        counters.clear()
        keyUses.clear()
    }

    private fun useKey(token: String, keyIndex: Int) = "$token/$keyIndex"
}
