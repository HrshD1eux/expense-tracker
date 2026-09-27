package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testIndianCurrencyFormatting() {
        // ₹1
        assertEquals("₹1", CurrencyFormatter.formatPaise(100L))

        // ₹1,240
        assertEquals("₹1,240", CurrencyFormatter.formatPaise(124000L))

        // ₹10,500
        assertEquals("₹10,500", CurrencyFormatter.formatPaise(1050000L))

        // ₹1,25,000 (Indian lakhs grouping: 1,25,000 instead of 125,000)
        assertEquals("₹1,25,000", CurrencyFormatter.formatPaise(12500000L))

        // ₹125.50
        assertEquals("₹125.50", CurrencyFormatter.formatPaise(12550L))
    }

    @Test
    fun testStandardCurrencyFormatting() {
        // $125,000 for USD
        assertEquals("$125,000", CurrencyFormatter.formatPaise(12500000L, symbol = "$", currencyCode = "USD"))
        assertEquals("$1,240", CurrencyFormatter.formatPaise(124000L, symbol = "$", currencyCode = "USD"))
    }
}
