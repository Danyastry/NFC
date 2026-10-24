package com.danyastry.nfcpay.core.emv.card

class StaticCardKeys(
    private val cardMasterKey: ByteArray,
    override val version: CryptogramVersion,
) : CardKeys {

    init {
        require(version != CryptogramVersion.LIMITED_USE_KEY) { "Limited-use keys come from a wallet, not from the card" }
    }

    private var transactionKey: ByteArray = cardMasterKey

    override val derivationKeyIndex: Int = CardCryptogram.DERIVATION_KEY_INDEX.toInt()

    override fun startTransaction(atc: Int): Boolean {
        transactionKey = CardCryptogram.key(cardMasterKey, version, atc)
        return true
    }

    override fun cryptogramKey(): ByteArray = transactionKey
}
