package com.danyastry.nfcpay.core.ndef.record

object UriPrefixes {

    val ALL: List<String> = listOf(
        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
        "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://",
        "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
        "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:",
    )

    fun abbreviate(uri: String): Pair<Int, String> {
        val best = ALL.indices.drop(1).filter { uri.startsWith(ALL[it]) }.maxByOrNull { ALL[it].length } ?: 0
        return best to uri.removePrefix(ALL[best])
    }

    fun expand(code: Int, rest: String): String = (ALL.getOrNull(code) ?: "") + rest
}
