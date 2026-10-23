package com.danyastry.nfcpay.core.emv.util

object Luhn {

    fun isValid(number: String): Boolean =
        number.length >= 2 && number.all { it in '0'..'9' } && checkDigit(number.dropLast(1)) == number.last()

    fun checkDigit(partial: String): Char {
        require(partial.isNotEmpty() && partial.all { it in '0'..'9' }) { "Luhn needs digits, got '$partial'" }
        val sum = partial.reversed().mapIndexed { index, char ->
            val digit = char - '0'
            if (index % 2 == 0) (digit * 2).let { if (it > 9) it - 9 else it } else digit
        }.sum()
        return '0' + (10 - sum % 10) % 10
    }
}
