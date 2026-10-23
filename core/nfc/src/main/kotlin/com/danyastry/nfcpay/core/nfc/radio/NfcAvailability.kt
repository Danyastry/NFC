package com.danyastry.nfcpay.core.nfc.radio

sealed interface NfcAvailability {
    data object NoNfc : NfcAvailability
    data object Off : NfcAvailability
    data class Ready(val canEmulateCards: Boolean) : NfcAvailability

    val canUseRadio: Boolean
        get() = this is Ready

    companion object {
        fun resolve(state: NfcState): NfcAvailability = when {
            !state.hasNfc -> NoNfc
            !state.isEnabled -> Off
            else -> Ready(canEmulateCards = state.hasCardEmulation)
        }
    }
}
