package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class DateTimeUtilsTest {

    private val utcZone = ZoneId.of("UTC")

    @Test
    fun testGreetings() {
        assertEquals("Good morning", DateTimeUtils.getGreeting(8))
        assertEquals("Good afternoon", DateTimeUtils.getGreeting(13))
        assertEquals("Good evening", DateTimeUtils.getGreeting(19))
        assertEquals("Good night", DateTimeUtils.getGreeting(23))
        assertEquals("Good night", DateTimeUtils.getGreeting(2))
    }

    @Test
    fun testDayBoundaries() {
        val testDate = LocalDate.of(2026, 9, 27)
        val startMillis = DateTimeUtils.getDayStartEpochMillis(testDate, utcZone)
        val endMillis = DateTimeUtils.getDayEndEpochMillis(testDate, utcZone)

        assertTrue(endMillis > startMillis)
        val dayDuration = endMillis - startMillis
        // Nearly 24 hours (86,400,000 ms - 1 nano/milli)
        assertTrue(dayDuration >= 86399000L && dayDuration <= 86400000L)

        val retrievedDate = DateTimeUtils.getLocalDate(startMillis, utcZone)
        assertEquals(testDate, retrievedDate)
    }

    @Test
    fun testMonthBoundaries() {
        val testDate = LocalDate.of(2026, 2, 15) // February 2026 (non-leap year, 28 days)
        val startMillis = DateTimeUtils.getMonthStartEpochMillis(testDate, utcZone)
        val endMillis = DateTimeUtils.getMonthEndEpochMillis(testDate, utcZone)

        val startDate = DateTimeUtils.getLocalDate(startMillis, utcZone)
        val endDate = DateTimeUtils.getLocalDate(endMillis, utcZone)

        assertEquals(LocalDate.of(2026, 2, 1), startDate)
        assertEquals(LocalDate.of(2026, 2, 28), endDate)
    }

    @Test
    fun testLeapYearMonthBoundaries() {
        val leapYearDate = LocalDate.of(2028, 2, 10) // February 2028 (leap year, 29 days)
        val endMillis = DateTimeUtils.getMonthEndEpochMillis(leapYearDate, utcZone)
        val endDate = DateTimeUtils.getLocalDate(endMillis, utcZone)

        assertEquals(LocalDate.of(2028, 2, 29), endDate)
    }

    @Test
    fun testWeekBoundaries() {
        // 2026-09-27 is Sunday
        val sunday = LocalDate.of(2026, 9, 27)
        val mondayStart = DateTimeUtils.getWeekStartEpochMillis(sunday, DayOfWeek.MONDAY, utcZone)
        val startDate = DateTimeUtils.getLocalDate(mondayStart, utcZone)

        // Previous Monday was 2026-09-21
        assertEquals(LocalDate.of(2026, 9, 21), startDate)
    }

    @Test
    fun testHistoryDateHeader_today() {
        // Use the system zone for "today" matching — same as the impl
        val todayMillis = DateTimeUtils.getDayStartEpochMillis()
        val header = DateTimeUtils.getHistoryDateHeader(todayMillis)
        assertTrue("Header should start with TODAY", header.startsWith("TODAY"))
    }

    @Test
    fun testHistoryDateHeader_yesterday() {
        val yesterdayMillis = DateTimeUtils.getDayStartEpochMillis(LocalDate.now().minusDays(1))
        val header = DateTimeUtils.getHistoryDateHeader(yesterdayMillis)
        assertTrue("Header should start with YESTERDAY", header.startsWith("YESTERDAY"))
    }

    @Test
    fun testHistoryDateHeader_sameYear_noYearShown() {
        // A date this year (2026) that is not today or yesterday — should show "D MON" without year
        val thisYearDate = LocalDate.of(2026, 1, 15)
        val millis = DateTimeUtils.getDayStartEpochMillis(thisYearDate, utcZone)
        val header = DateTimeUtils.getHistoryDateHeader(millis, utcZone)
        // Should be "15 JAN" — no "2026"
        assertTrue("Same-year header should not contain year: $header", !header.contains("2026"))
        assertTrue("Same-year header should contain month: $header", header.contains("JAN"))
    }

    @Test
    fun testHistoryDateHeader_differentYear_showsYear() {
        // A date from a different year should include the year
        val oldDate = LocalDate.of(2024, 6, 10)
        val millis = DateTimeUtils.getDayStartEpochMillis(oldDate, utcZone)
        val header = DateTimeUtils.getHistoryDateHeader(millis, utcZone)
        assertTrue("Cross-year header should contain year: $header", header.contains("2024"))
    }

    @Test
    fun testMidnightHandling() {
        val date = LocalDate.of(2026, 7, 4)
        val startMillis = DateTimeUtils.getDayStartEpochMillis(date, utcZone)
        val endMillis = DateTimeUtils.getDayEndEpochMillis(date, utcZone)

        // Start must be exactly midnight (00:00:00)
        assertEquals(0, DateTimeUtils.getHourOfDay(startMillis, utcZone))
        assertEquals(date, DateTimeUtils.getLocalDate(startMillis, utcZone))

        // Millisecond before next day's midnight belongs to date
        assertEquals(date, DateTimeUtils.getLocalDate(endMillis, utcZone))
        assertEquals(23, DateTimeUtils.getHourOfDay(endMillis, utcZone))
    }

    @Test
    fun testYearBoundaries() {
        val dec31 = LocalDate.of(2026, 12, 31)
        val jan1 = LocalDate.of(2027, 1, 1)

        val endOfYearMillis = DateTimeUtils.getDayEndEpochMillis(dec31, utcZone)
        val startOfNewYearMillis = DateTimeUtils.getDayStartEpochMillis(jan1, utcZone)

        assertEquals(dec31, DateTimeUtils.getLocalDate(endOfYearMillis, utcZone))
        assertEquals(jan1, DateTimeUtils.getLocalDate(startOfNewYearMillis, utcZone))
        assertTrue("New year starts right after old year ends", startOfNewYearMillis > endOfYearMillis)
        assertTrue("Difference between end of day and next start is under 2ms", startOfNewYearMillis - endOfYearMillis <= 2L)
    }

    @Test
    fun testFebruaryLeapYear_vs_CommonYear() {
        // 2024 was leap year (29 days)
        val feb2024 = LocalDate.of(2024, 2, 10)
        val end2024 = DateTimeUtils.getLocalDate(DateTimeUtils.getMonthEndEpochMillis(feb2024, utcZone), utcZone)
        assertEquals(LocalDate.of(2024, 2, 29), end2024)

        // 2025 was common year (28 days)
        val feb2025 = LocalDate.of(2025, 2, 10)
        val end2025 = DateTimeUtils.getLocalDate(DateTimeUtils.getMonthEndEpochMillis(feb2025, utcZone), utcZone)
        assertEquals(LocalDate.of(2025, 2, 28), end2025)

        // 2028 will be leap year (29 days)
        val feb2028 = LocalDate.of(2028, 2, 10)
        val end2028 = DateTimeUtils.getLocalDate(DateTimeUtils.getMonthEndEpochMillis(feb2028, utcZone), utcZone)
        assertEquals(LocalDate.of(2028, 2, 29), end2028)
    }

    @Test
    fun testTimezoneHandling_KolkataVsUtc() {
        val kolkataZone = ZoneId.of("Asia/Kolkata") // UTC+5:30
        val date = LocalDate.of(2026, 9, 27)

        val startKolkata = DateTimeUtils.getDayStartEpochMillis(date, kolkataZone)
        val startUtc = DateTimeUtils.getDayStartEpochMillis(date, utcZone)

        // Kolkata midnight is 5.5 hours (19,800,000 ms) earlier than UTC midnight
        assertEquals(19800000L, startUtc - startKolkata)
    }
}
