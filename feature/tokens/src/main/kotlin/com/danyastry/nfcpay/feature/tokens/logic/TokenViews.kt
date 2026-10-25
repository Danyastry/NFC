package com.danyastry.nfcpay.feature.tokens.logic

import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.token.TokenRecord
import com.danyastry.nfcpay.core.emv.token.TokenStatus
import com.danyastry.nfcpay.core.nfc.wallet.WalletState
import com.danyastry.nfcpay.feature.tokens.model.VaultEntry
import com.danyastry.nfcpay.feature.tokens.model.WalletView
import java.time.format.DateTimeFormatter

object TokenViews {

    private val EXPIRY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yy")

    fun wallet(state: WalletState): WalletView = WalletView(
        token = SensitiveData.groupPan(state.token.token),
        account = SensitiveData.maskPan(state.token.pan),
        expiry = state.token.expiry.format(EXPIRY_FORMAT),
        paymentAccountReference = state.token.paymentAccountReference,
        requestorId = state.token.requestorId,
        status = status(state.token.status),
        isActive = state.token.status == TokenStatus.ACTIVE,
        isSuspended = state.token.status == TokenStatus.SUSPENDED,
        keyIndex = state.keyIndex,
        keyCheckValue = state.keyCheckValue,
        tapsLeft = state.tapsLeft,
        tapsPerKey = state.tapsPerKey,
        atc = state.atc,
    )

    fun vault(record: TokenRecord): VaultEntry = VaultEntry(
        token = SensitiveData.groupPan(record.token),
        domain = record.domain.label,
        status = status(record.status),
    )

    fun status(status: TokenStatus): String = when (status) {
        TokenStatus.ACTIVE -> "Active"
        TokenStatus.SUSPENDED -> "Suspended"
        TokenStatus.DELETED -> "Deleted"
    }
}
