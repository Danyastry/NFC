package com.danyastry.nfcpay.core.nfc.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun ReaderModeEffect(enabled: Boolean, onCard: (IsoDep) -> Unit) {
    val context = LocalContext.current
    val currentOnCard by rememberUpdatedState(onCard)
    LifecycleResumeEffect(enabled, context) {
        val activity = context.findActivity()
        val adapter = NfcAdapter.getDefaultAdapter(context)
        if (enabled && activity != null && adapter != null) {
            val extras = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_MILLIS) }
            adapter.enableReaderMode(activity, { tag -> IsoDep.get(tag)?.let { currentOnCard(it) } }, READER_FLAGS, extras)
        }
        onPauseOrDispose {
            if (enabled && activity != null && adapter != null) adapter.disableReaderMode(activity)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val PRESENCE_CHECK_MILLIS = 250
private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
