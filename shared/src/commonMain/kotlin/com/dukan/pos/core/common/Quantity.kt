package com.dukan.pos.core.common

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Stock quantity formatting. Kiryana stock is mixed: 12 packets (whole) but
 * 1.75 kg daal (fractional). Show decimals only when the value has them.
 */
fun Double.formatQty(maxDecimals: Int = 3): String {
    if (abs(this - roundToLong()) < 0.0005) return roundToLong().toString()
    var text = toString()
    if (text.contains('.')) {
        val decimals = text.substringAfter('.')
        if (decimals.length > maxDecimals) {
            val factor = generateSequence(1.0) { it * 10 }.elementAt(maxDecimals)
            text = ((this * factor).roundToLong() / factor).toString()
        }
        text = text.trimEnd('0').trimEnd('.')
    }
    return text
}

fun String.toQtyOrNull(): Double? {
    val cleaned = trim().replace(",", "")
    if (cleaned.isEmpty()) return null
    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
}
