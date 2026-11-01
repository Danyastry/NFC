package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.card.CryptogramVersion
import com.danyastry.nfcpay.core.emv.card.IssuerApplicationData

interface IssuerCredential {
    val account: IssuerAccount

    val pinBlockPan: String

    val lastAtc: Int

    fun accepts(version: CryptogramVersion): Boolean

    fun cryptogramKey(iad: IssuerApplicationData, atc: Int, step: (title: String, detail: String) -> Unit): ByteArray

    fun counted(atc: Int): IssuerAccount
}
