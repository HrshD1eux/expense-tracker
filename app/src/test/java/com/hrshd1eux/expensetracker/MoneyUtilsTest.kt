package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.util.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyUtilsTest {

    @Test
    fun testParseAmountToPaise_wholeNumbers() {
        assertEquals(10000L, MoneyUtils.parseAmountToPaise("100"))
        assertEquals(25000L, MoneyUtils.parseAmountToPaise("250"))
        assertEquals(100L, MoneyUtils.parseAmountToPaise("1"))
        assertEquals(0L, MoneyUtils.parseAmountToPaise("0"))
    }

    @Test
    fun testParseAmountToPaise_decimals() {
        assertEquals(12550L, MoneyUtils.parseAmountToPaise("125.50"))
        assertEquals(12550L, MoneyUtils.parseAmountToPaise("125.5"))
        assertEquals(1025L, MoneyUtils.parseAmountToPaise("10.25"))
        assertEquals(75L, MoneyUtils.parseAmountToPaise("0.75"))
        assertEquals(5L, MoneyUtils.parseAmountToPaise("0.05"))
    }

    @Test
    fun testExactPaiseAddition_preventsFloatingPointInaccuracy() {
        // ₹100 + ₹200 = ₹300
        val p1 = MoneyUtils.parseAmountToPaise("100")
        val p2 = MoneyUtils.parseAmountToPaise("200")
        assertEquals(30000L, p1 + p2)

        // ₹125.50 + ₹10.25 = ₹135.75
        val a = MoneyUtils.parseAmountToPaise("125.50")
        val b = MoneyUtils.parseAmountToPaise("10.25")
        val sum = a + b
        assertEquals(13575L, sum)
        assertEquals("135.75", MoneyUtils.formatPaiseToEditable(sum))
    }

    @Test
    fun testCalculateDailyAverage() {
        assertEquals(5000L, MoneyUtils.calculateDailyAverage(15000L, 3))
        assertEquals(0L, MoneyUtils.calculateDailyAverage(0L, 5))
        assertEquals(0L, MoneyUtils.calculateDailyAverage(1000L, 0))
    }

    @Test
    fun testFormatPaiseToEditable() {
        assertEquals("250", MoneyUtils.formatPaiseToEditable(25000L))
        assertEquals("125.50", MoneyUtils.formatPaiseToEditable(12550L))
        assertEquals("0.75", MoneyUtils.formatPaiseToEditable(75L))
        assertEquals("", MoneyUtils.formatPaiseToEditable(0L))
    }

    @Test
    fun testEvaluateExpression_additionAndSubtraction() {
        // "45 + 120 + 85" = 250 -> 25000 paise
        assertEquals(25000L, MoneyUtils.evaluateExpressionToPaise("45 + 120 + 85"))
        // "350 + 40" = 390 -> 39000 paise
        assertEquals(39000L, MoneyUtils.evaluateExpressionToPaise("350 + 40"))
        // "500 - 150" = 350 -> 35000 paise
        assertEquals(35000L, MoneyUtils.evaluateExpressionToPaise("500 - 150"))
    }

    @Test
    fun testEvaluateExpression_multiplicationAndDivision() {
        // "1450 / 3" = 483.33 -> 48333 paise
        assertEquals(48333L, MoneyUtils.evaluateExpressionToPaise("1450 / 3"))
        assertEquals("483.33", MoneyUtils.evaluateExpressionToString("1450 / 3"))

        // "25 * 4" = 100 -> 10000 paise
        assertEquals(10000L, MoneyUtils.evaluateExpressionToPaise("25 * 4"))
        // "25 × 4" = 100
        assertEquals(10000L, MoneyUtils.evaluateExpressionToPaise("25 × 4"))
        // "100 ÷ 4" = 25
        assertEquals(2500L, MoneyUtils.evaluateExpressionToPaise("100 ÷ 4"))
    }

    @Test
    fun testEvaluateExpression_precedence() {
        // "10 + 20 * 2" = 10 + 40 = 50 -> 5000 paise
        assertEquals(5000L, MoneyUtils.evaluateExpressionToPaise("10 + 20 * 2"))
    }

    @Test
    fun testEvaluateExpression_invalidOrIncomplete() {
        // Incomplete expression ends with operator
        org.junit.Assert.assertNull(MoneyUtils.evaluateExpressionToPaise("100 +"))
        // Division by zero
        org.junit.Assert.assertNull(MoneyUtils.evaluateExpressionToPaise("100 / 0"))
    }
}
