package com.danyastry.nfcpay.core.emv.dol

import com.danyastry.nfcpay.core.emv.tlv.Tag

data class DolEntry(val tag: Tag, val length: Int)
