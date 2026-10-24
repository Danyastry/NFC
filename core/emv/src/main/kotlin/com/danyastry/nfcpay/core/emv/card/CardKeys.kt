package com.danyastry.nfcpay.core.emv.card

interface CardKeys {
    val version: CryptogramVersion
    val derivationKeyIndex: Int

    fun startTransaction(atc: Int): Boolean

    fun cryptogramKey(): ByteArray
}
