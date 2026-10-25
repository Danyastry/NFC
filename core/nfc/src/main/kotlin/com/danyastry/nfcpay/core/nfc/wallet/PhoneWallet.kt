package com.danyastry.nfcpay.core.nfc.wallet

import com.danyastry.nfcpay.core.emv.card.ApduHandler
import com.danyastry.nfcpay.core.emv.card.CardProfile
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import com.danyastry.nfcpay.core.emv.token.TokenRecord
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenStatus
import com.danyastry.nfcpay.core.emv.token.WalletKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

class PhoneWallet(
    val service: TokenService,
    private val account: CardProfile = DemoCards.lab,
    private val tokenExpiry: () -> YearMonth = { YearMonth.now().plusYears(TOKEN_YEARS) },
) {
    private var card: VirtualCard? = null
    private var keys: WalletKeys? = null
    private var token: String? = null

    private val _state = MutableStateFlow<WalletState?>(null)
    val state: StateFlow<WalletState?> = _state.asStateFlow()

    private val _tokens = MutableStateFlow<List<TokenRecord>>(emptyList())
    val tokens: StateFlow<List<TokenRecord>> = _tokens.asStateFlow()

    @Synchronized
    fun addCard() {
        if (card != null) return
        val provisioning = service.provisionWallet(account.pan, account.panSequenceNumber, DemoCards.walletRequestorId, tokenExpiry())
        val walletKeys = WalletKeys(provisioning.firstKey)
        keys = walletKeys
        token = provisioning.token.token
        card = VirtualCard(DemoCards.tokenCard(provisioning.token), keys = walletKeys)
        publish()
    }

    @Synchronized
    fun handler(): ApduHandler? {
        val current = card ?: return null
        return ApduHandler { command -> current.process(command).also { publish() } }
    }

    @Synchronized
    fun replenish() {
        val number = token ?: return
        keys?.replenish(service.replenish(number))
        publish()
    }

    @Synchronized
    fun suspend() {
        token?.let(service.vault::suspend)
        publish()
    }

    @Synchronized
    fun resume() {
        token?.let(service.vault::resume)
        publish()
    }

    @Synchronized
    fun remove() {
        token?.let(service.vault::delete)
        card = null
        keys = null
        token = null
        publish()
    }

    @Synchronized
    private fun publish() {
        _tokens.value = service.vault.tokensFor(account.pan)
        val record = token?.let(service.vault::record)
        val walletKeys = keys
        val walletCard = card
        _state.value = if (record != null && walletKeys != null && walletCard != null && record.status != TokenStatus.DELETED) {
            WalletState(
                token = record,
                keyIndex = walletKeys.current.index,
                keyCheckValue = walletKeys.current.checkValue,
                tapsLeft = walletKeys.current.usesLeft,
                tapsPerKey = service.usesPerKey,
                atc = walletCard.atc,
            )
        } else {
            null
        }
    }

    companion object {
        const val TOKEN_YEARS = 3L
    }
}
