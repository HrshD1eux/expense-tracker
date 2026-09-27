package com.hrshd1eux.expensetracker.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateTimeUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    fun currentEpochMillis(): Long = System.currentTimeMillis()

    fun getGreeting(hour: Int = LocalDateTime.now().hour): String {
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun formatCurrentHeaderDate(localDate: LocalDate = LocalDate.now()): String {
        return localDate.format(fullDateFormatter)
    }

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return LocalDateTime.ofInstant(instant, zoneId).format(timeFormatter)
    }

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return LocalDateTime.ofInstant(instant, zoneId).format(dateOnlyFormatter)
    }

    fun formatShortDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        return LocalDateTime.ofInstant(instant, zoneId).format(shortDateFormatter)
    }

    fun getDayStartEpochMillis(
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getDayEndEpochMillis(
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun getWeekStartEpochMillis(
        date: LocalDate = LocalDate.now(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val weekStart = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getWeekEndEpochMillis(
        date: LocalDate = LocalDate.now(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val lastDayOfWeek = firstDayOfWeek.plus(6)
        val weekEnd = date.with(TemporalAdjusters.nextOrSame(lastDayOfWeek))
        return weekEnd.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun getMonthStartEpochMillis(
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val monthStart = date.with(TemporalAdjusters.firstDayOfMonth())
        return monthStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getMonthEndEpochMillis(
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val monthEnd = date.with(TemporalAdjusters.lastDayOfMonth())
        return monthEnd.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun getLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    }

    fun getHourOfDay(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).hour
    }

    fun getHistoryDateHeader(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val expenseDate = getLocalDate(epochMillis, zoneId)
        val today = LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        return when (expenseDate) {
            today -> "TODAY · " + expenseDate.format(shortDateFormatter).uppercase()
            yesterday -> "YESTERDAY · " + expenseDate.format(shortDateFormatter).uppercase()
            else -> {
                // Show year only when it differs from the current year (e.g. viewing old expenses)
                val fmt = if (expenseDate.year != today.year) dateOnlyFormatter else shortDateFormatter
                expenseDate.format(fmt).uppercase()
            }
        }
    }

    fun toIsoString(epochMillis: Long): String {
        return isoFormatter.format(Instant.ofEpochMilli(epochMillis))
    }

    fun parseIsoToEpochMillis(isoString: String): Long {
        return Instant.parse(isoString).toEpochMilli()
    }
}
