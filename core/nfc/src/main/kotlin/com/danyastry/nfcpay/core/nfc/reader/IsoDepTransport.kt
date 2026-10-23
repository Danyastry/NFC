package com.danyastry.nfcpay.core.nfc.reader

import android.nfc.tech.IsoDep
import com.danyastry.nfcpay.core.emv.transport.CardTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

class IsoDepTransport private constructor(private val isoDep: IsoDep) : CardTransport, Closeable {

    override suspend fun transceive(command: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        isoDep.transceive(command)
    }

    override fun close() {
        runCatching { isoDep.close() }
    }

    companion object {
        const val TIMEOUT_MILLIS = 5_000

        suspend fun open(isoDep: IsoDep): IsoDepTransport = withContext(Dispatchers.IO) {
            isoDep.connect()
            isoDep.timeout = TIMEOUT_MILLIS
            IsoDepTransport(isoDep)
        }
    }
}
