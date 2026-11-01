package com.danyastry.nfcpay.feature.card.logic

import com.danyastry.nfcpay.core.emv.apdu.CommandDescriber
import com.danyastry.nfcpay.core.emv.apdu.ResponseApdu
import com.danyastry.nfcpay.core.emv.card.CardProfile
import com.danyastry.nfcpay.core.emv.mask.SensitiveData
import com.danyastry.nfcpay.core.emv.util.toHex
import com.danyastry.nfcpay.core.nfc.hce.ServedApdu
import com.danyastry.nfcpay.feature.card.model.CardDetails
import com.danyastry.nfcpay.feature.card.model.ServedEntry
import java.time.format.DateTimeFormatter

object ServedEntries {

    private val EXPIRY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yy")

    fun map(served: ServedApdu): ServedEntry {
        val response = runCatching { ResponseApdu.parse(served.response) }.getOrNull()
        return ServedEntry(
            label = CommandDescriber.describe(served.command),
            source = served.source,
            command = served.command.toHex(),
            response = served.response.toHex(),
            sw = response?.sw,
            timeMillis = served.timeMillis,
        )
    }

    fun details(profile: CardProfile): CardDetails = CardDetails(
        label = profile.label,
        pan = SensitiveData.groupPan(profile.pan),
        expiry = profile.expiry.format(EXPIRY_FORMAT),
        cardholderName = profile.cardholderName,
        aid = profile.aid.toHex(),
    )
}
