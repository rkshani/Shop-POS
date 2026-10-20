package com.dukan.pos.core.common

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Money is stored as an integer number of paisa. Never use Double for money:
 * 0.1 + 0.2 != 0.3 and a kiryana shop's khata cannot drift by a paisa a day.
 */
@JvmInline
value class Money(val paisa: Long) : Comparable<Money> {

    val rupees: Double get() = paisa / 100.0

    operator fun plus(other: Money) = Money(paisa + other.paisa)

    operator fun minus(other: Money) = Money(paisa - other.paisa)

    operator fun times(quantity: Double) = Money((paisa * quantity).roundToLong())

    operator fun times(quantity: Int) = Money(paisa * quantity)

    operator fun unaryMinus() = Money(-paisa)

    override fun compareTo(other: Money) = paisa.compareTo(other.paisa)

    val isZero: Boolean get() = paisa == 0L
    val isPositive: Boolean get() = paisa > 0L
    val isNegative: Boolean get() = paisa < 0L

    /** "Rs 1,250" or "Rs 1,250.50" — decimals only shown when they exist. */
    fun format(symbol: String = "Rs", alwaysShowDecimals: Boolean = false): String {
        val negative = paisa < 0
        val absolute = abs(paisa)
        val whole = absolute / 100
        val fraction = (absolute % 100).toInt()

        val grouped = groupDigits(whole)
        val body = if (fraction == 0 && !alwaysShowDecimals) {
            grouped
        } else {
            "$grouped.${fraction.toString().padStart(2, '0')}"
        }
        return buildString {
            if (negative) append('-')
            if (symbol.isNotEmpty()) {
                append(symbol)
                append(' ')
            }
            append(body)
        }
    }

    companion object {
        val ZERO = Money(0)

        fun fromRupees(value: Double) = Money((value * 100).roundToLong())

        fun fromRupees(value: Long) = Money(value * 100)

        /** Parses user input like "1,250.50" / "1250" / "" leniently. */
        fun parseOrNull(input: String): Money? {
            val cleaned = input.trim().replace(",", "").removePrefix("Rs").trim()
            if (cleaned.isEmpty()) return null
            val value = cleaned.toDoubleOrNull() ?: return null
            return fromRupees(value)
        }
    }
}

private fun groupDigits(value: Long): String {
    val text = value.toString()
    if (text.length <= 3) return text
    return text.reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { acc, money -> acc + money }
