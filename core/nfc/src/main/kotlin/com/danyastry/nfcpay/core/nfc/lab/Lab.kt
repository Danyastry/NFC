package com.danyastry.nfcpay.core.nfc.lab

import com.danyastry.nfcpay.core.emv.card.ApduHandler
import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.dukpt.DukptTerminal
import com.danyastry.nfcpay.core.emv.hsm.DemoHsm
import com.danyastry.nfcpay.core.emv.hsm.PinService
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.danyastry.nfcpay.core.emv.terminal.PinPad
import com.danyastry.nfcpay.core.emv.terminal.TerminalKernel
import com.danyastry.nfcpay.core.emv.token.TokenService
import com.danyastry.nfcpay.core.emv.token.TokenVault
import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.type4.Type4Tag
import com.danyastry.nfcpay.core.nfc.hce.ApduSource
import com.danyastry.nfcpay.core.nfc.hce.CardHost
import com.danyastry.nfcpay.core.nfc.issuer.IssuerLink
import com.danyastry.nfcpay.core.nfc.wallet.PhoneWallet

class Lab(tokenVault: TokenVault = TokenVault()) {

    val pins: PinService = DemoHsm.pinService()
    private val pinDevice: DukptTerminal = DemoHsm.pinPadDevice()
    val tokens: TokenService = TokenService(tokenVault, DemoCards.issuerMasterKey)
    val wallet: PhoneWallet = PhoneWallet(tokens)
    val issuer: IssuerLink = IssuerLink(DemoIssuer.create(tokens, pins), DemoIssuer.account())
    val card: CardHost = CardHost(DemoCards.lab)
    val tag: Type4Tag = Type4Tag(initial = WELCOME)

    fun pinPad(prompt: suspend (pan: String, amount: Long) -> String?): PinPad =
        PinPad { pan, amount -> prompt(pan, amount)?.let { pinDevice.encryptPin(it, pan) } }

    fun terminal(source: CardSource, pinPad: PinPad? = null): TerminalKernel = when (source) {
        CardSource.SIMULATED -> TerminalKernel(authorizer = issuer.authorizer, pinPad = pinPad)
        CardSource.WALLET -> TerminalKernel(authorizer = issuer.authorizer)
        CardSource.NFC -> TerminalKernel()
    }

    fun loopbackCard(source: CardSource): ApduHandler? = when (source) {
        CardSource.SIMULATED -> card.handler(ApduSource.SIMULATED)
        CardSource.WALLET -> wallet.handler()
        CardSource.NFC -> null
    }

    private companion object {
        val WELCOME = NdefMessage(TextRecord("Hello from NFC Pay Lab").toRecord())
    }
}
