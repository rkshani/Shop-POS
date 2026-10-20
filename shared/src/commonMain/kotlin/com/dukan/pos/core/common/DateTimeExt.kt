package com.dukan.pos.core.common

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

interface Clock {
    fun now(): Instant
    fun timeZone(): TimeZone
}

class SystemClock(private val zone: TimeZone = TimeZone.currentSystemDefault()) : Clock {
    override fun now(): Instant = kotlinx.datetime.Clock.System.now()
    override fun timeZone(): TimeZone = zone
}

fun Instant.toLocalDate(zone: TimeZone): LocalDate = toLocalDateTime(zone).date

fun LocalDate.startOfDay(zone: TimeZone): Instant = atStartOfDayIn(zone)

fun LocalDate.endOfDayExclusive(zone: TimeZone): Instant = plusDays(1).atStartOfDayIn(zone)

fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)

/** "28 Jul 2026" */
fun LocalDate.formatShort(): String = "$dayOfMonth ${monthShortName()} $year"

/** "28 Jul, 3:40 PM" */
fun LocalDateTime.formatShort(): String {
    val hour12 = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    val marker = if (hour < 12) "AM" else "PM"
    return "$dayOfMonth ${date.monthShortName()}, $hour12:${minute.toString().padStart(2, '0')} $marker"
}

private fun LocalDate.monthShortName(): String = when (monthNumber) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
}
