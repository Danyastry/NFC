package com.danyastry.nfcpay.core.emv.token

import com.danyastry.nfcpay.core.emv.card.CardKeys
import com.danyastry.nfcpay.core.emv.card.CryptogramVersion

class WalletKeys(initial: LimitedUseKey) : CardKeys {

    var current: LimitedUseKey = initial
        private set

    private var transactionKey: LimitedUseKey? = null

    override val version: CryptogramVersion = CryptogramVersion.LIMITED_USE_KEY

    override val derivationKeyIndex: Int
        get() = (transactionKey ?: current).index

    val needsReplenishment: Boolean
        @Synchronized get() = current.usesLeft == 0

    @Synchronized
    override fun startTransaction(atc: Int): Boolean {
        if (current.usesLeft == 0) return false
        transactionKey = current
        current = current.used()
        return true
    }

    @Synchronized
    override fun cryptogramKey(): ByteArray = checkNotNull(transactionKey) { "No transaction in progress" }.key

    @Synchronized
    fun replenish(next: LimitedUseKey) {
        require(next.index > current.index) { "A new key must have a higher index than ${current.index}, got ${next.index}" }
        current = next
    }
}
