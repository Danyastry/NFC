package com.danyastry.nfcpay.core.nfc.hce

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation

object CardEmulationStatus {

    fun service(context: Context): ComponentName = ComponentName(context, PaymentCardService::class.java)

    fun isDefaultPaymentService(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return runCatching {
            CardEmulation.getInstance(adapter).isDefaultServiceForCategory(service(context), CardEmulation.CATEGORY_PAYMENT)
        }.getOrDefault(false)
    }

    fun requestDefaultIntent(context: Context): Intent =
        Intent(CardEmulation.ACTION_CHANGE_DEFAULT)
            .putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT)
            .putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, service(context))
}
