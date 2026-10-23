package com.danyastry.nfcpay.core.emv.apdu

object StatusWord {
    const val SUCCESS = 0x9000
    const val WRONG_LENGTH = 0x6700
    const val SECURITY_STATUS_NOT_SATISFIED = 0x6982
    const val CONDITIONS_NOT_SATISFIED = 0x6985
    const val FUNCTION_NOT_SUPPORTED = 0x6A81
    const val FILE_NOT_FOUND = 0x6A82
    const val RECORD_NOT_FOUND = 0x6A83
    const val INCORRECT_P1_P2 = 0x6A86
    const val REFERENCED_DATA_NOT_FOUND = 0x6A88
    const val INS_NOT_SUPPORTED = 0x6D00
    const val CLA_NOT_SUPPORTED = 0x6E00
    const val UNKNOWN_ERROR = 0x6F00

    private val descriptions = mapOf(
        SUCCESS to "Success",
        WRONG_LENGTH to "Wrong length",
        SECURITY_STATUS_NOT_SATISFIED to "Security status not satisfied",
        CONDITIONS_NOT_SATISFIED to "Conditions of use not satisfied",
        FUNCTION_NOT_SUPPORTED to "Function not supported",
        FILE_NOT_FOUND to "File or application not found",
        RECORD_NOT_FOUND to "Record not found",
        INCORRECT_P1_P2 to "Incorrect P1 or P2",
        REFERENCED_DATA_NOT_FOUND to "Referenced data not found",
        INS_NOT_SUPPORTED to "Instruction not supported",
        CLA_NOT_SUPPORTED to "Class not supported",
        UNKNOWN_ERROR to "No precise diagnosis",
    )

    fun describe(sw: Int): String = when {
        sw in descriptions -> descriptions.getValue(sw)
        sw ushr 8 == 0x61 -> "${sw and 0xFF} more bytes available"
        sw ushr 8 == 0x6C -> "Wrong Le, retry with ${sw and 0xFF}"
        sw and 0xFFF0 == 0x63C0 -> "Verification failed, ${sw and 0x0F} tries left"
        else -> "Unknown status"
    }
}
