package com.danyastry.nfcpay.core.nfc.radio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NfcStatus {

    fun adapter(context: Context): NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun current(context: Context): NfcState {
        val adapter = adapter(context)
        val packageManager = context.packageManager
        return NfcState(
            hasNfc = adapter != null && packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            hasCardEmulation = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION),
            isEnabled = adapter?.isEnabled == true,
        )
    }

    fun changes(context: Context): Flow<NfcState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(current(context))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        trySend(current(context))
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
