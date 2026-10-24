package com.danyastry.nfcpay.feature.issuer.logic

import com.danyastry.nfcpay.core.emv.crypto.IccKeyDerivation
import com.danyastry.nfcpay.core.emv.crypto.KeyCheckValue
import com.danyastry.nfcpay.core.emv.issuer.IssuerAccount
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.nfc.issuer.IssuerEvent
import com.danyastry.nfcpay.feature.issuer.model.AccountView
import com.danyastry.nfcpay.feature.issuer.model.AuthorizationView
import com.danyastry.nfcpay.feature.issuer.model.KeyHierarchy

object IssuerViews {

    fun money(minor: Long): String = "%d.%02d".format(minor / 100, minor % 100)

    fun account(account: IssuerAccount): AccountView = AccountView(
        pan = SensitiveData.groupPan(account.pan),
        balance = money(account.balance),
        lastAtc = account.lastAtc.takeIf { it != IssuerAccount.NO_TRANSACTIONS }?.toString() ?: "none",
    )

    fun keys(issuerMasterKey: ByteArray, account: IssuerAccount): KeyHierarchy = KeyHierarchy(
        issuerMasterKeyCheck = KeyCheckValue.of(issuerMasterKey),
        cardKeyCheck = KeyCheckValue.of(IccKeyDerivation.optionA(issuerMasterKey, account.pan, account.panSequenceNumber)),
        cardKeyDerivation = "Option A · PAN and sequence ${"%02d".format(account.panSequenceNumber)}",
    )

    fun authorization(event: IssuerEvent): AuthorizationView {
        val response = event.response
        return AuthorizationView(
            responseCode = response.responseCode.code,
            description = response.declineReason?.description ?: response.responseCode.description,
            isApproved = response.isApproved,
            isReplay = event.isReplay,
            amount = response.amount?.let(::money),
            atc = response.atc,
            steps = response.steps,
            timeMillis = event.timeMillis,
        )
    }
}
