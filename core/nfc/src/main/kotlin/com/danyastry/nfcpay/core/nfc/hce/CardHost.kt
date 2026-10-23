package com.danyastry.nfcpay.core.nfc.hce

import com.danyastry.nfcpay.core.emv.card.ApduHandler
import com.danyastry.nfcpay.core.emv.card.CardProfile
import com.danyastry.nfcpay.core.emv.card.VirtualCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CardHost(
    val profile: CardProfile,
    private val clock: () -> Long = System::currentTimeMillis,
    private val logLimit: Int = DEFAULT_LOG_LIMIT,
) {
    private val card = VirtualCard(profile)

    private val _atc = MutableStateFlow(card.atc)
    val atc: StateFlow<Int> = _atc.asStateFlow()

    private val _log = MutableStateFlow<List<ServedApdu>>(emptyList())
    val log: StateFlow<List<ServedApdu>> = _log.asStateFlow()

    fun process(command: ByteArray, source: ApduSource): ByteArray {
        val response = card.process(command)
        _atc.value = card.atc
        _log.update { (it + ServedApdu(source, command, response, clock())).takeLast(logLimit) }
        return response
    }

    fun handler(source: ApduSource): ApduHandler = ApduHandler { process(it, source) }

    fun deactivate() = card.deactivate()

    fun resetCounter() {
        card.resetCounter()
        _atc.value = card.atc
    }

    fun clearLog() {
        _log.value = emptyList()
    }

    companion object {
        const val DEFAULT_LOG_LIMIT = 100
    }
}
