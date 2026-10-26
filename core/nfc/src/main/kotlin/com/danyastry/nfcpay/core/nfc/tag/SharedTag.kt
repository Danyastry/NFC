package com.danyastry.nfcpay.core.nfc.tag

import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.type4.Type4Tag

object SharedTag {
    val tag: Type4Tag = Type4Tag(initial = NdefMessage(TextRecord("Hello from NFC Pay Lab").toRecord()))
}
