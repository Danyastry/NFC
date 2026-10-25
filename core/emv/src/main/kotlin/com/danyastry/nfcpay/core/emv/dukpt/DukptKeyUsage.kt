package com.danyastry.nfcpay.core.emv.dukpt

enum class DukptKeyUsage(val code: Int, val label: String) {
    PIN_ENCRYPTION(0x1000, "PIN encryption"),
    MAC_GENERATION(0x2000, "MAC generation"),
    MAC_VERIFICATION(0x2001, "MAC verification"),
    DATA_ENCRYPTION(0x3000, "Data encryption"),
    DATA_DECRYPTION(0x3001, "Data decryption"),
    KEY_DERIVATION(0x8000, "Key derivation"),
    INITIAL_KEY(0x8001, "Initial key derivation"),
}
