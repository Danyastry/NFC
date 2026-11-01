package com.danyastry.nfcpay.core.emv.util

object Money {
    fun format(minor: Long): String = "%d.%02d".format(minor / 100, minor % 100)
}
