package com.danyastry.nfcpay.core.ndef.type4

enum class TagError(val description: String) {
    NOT_AN_NDEF_TAG("The tag has no NDEF application"),
    NO_CAPABILITY_CONTAINER("The tag has no capability container"),
    BAD_CAPABILITY_CONTAINER("The capability container is malformed"),
    NO_NDEF_FILE("The NDEF file could not be selected"),
    READ_FAILED("Reading the tag failed"),
    EMPTY("The tag holds no message"),
    MALFORMED_MESSAGE("The tag holds bytes that are not a valid NDEF message"),
    READ_ONLY("The tag is read-only"),
    TOO_LARGE("The message does not fit on the tag"),
    WRITE_FAILED("Writing the tag failed"),
    TAG_LOST("The tag left the field"),
}
