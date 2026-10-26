package com.danyastry.nfcpay.core.nfc.tag

import android.nfc.Tag
import android.nfc.tech.Ndef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NdefTagReader {

    suspend fun read(tag: Tag): ByteArray? = withContext(Dispatchers.IO) {
        val ndef = Ndef.get(tag) ?: return@withContext null
        try {
            ndef.connect()
            ndef.ndefMessage?.toByteArray()
        } finally {
            runCatching { ndef.close() }
        }
    }
}
