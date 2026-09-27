package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.data.local.dao.CategorySpendAggregate
import com.hrshd1eux.expensetracker.data.local.dao.PaymentMethodSpendAggregate
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.domain.usecase.AnalyticsPeriod
import com.hrshd1eux.expensetracker.domain.usecase.GetAnalyticsUseCase
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AnalyticsUseCaseTest {

    private lateinit var fakeExpenseRepo: FakeAnalyticsExpenseRepository
    private lateinit var analyticsUseCase: GetAnalyticsUseCase

    private val foodCategory = Category(id = "cat_food", name = "Food", icon = "restaurant", color = 0xFFFB8C00, isDefault = true)
    private val transportCategory = Category(id = "cat_transport", name = "Transport", icon = "directions_transit", color = 0xFF1E88E5, isDefault = true)
    private val billsCategory = Category(id = "cat_bills", name = "Bills", icon = "receipt_long", color = 0xFF546E7A, isDefault = true)

    @Before
    fun setup() {
        fakeExpenseRepo = FakeAnalyticsExpenseRepository()
        analyticsUseCase = GetAnalyticsUseCase(fakeExpenseRepo)
    }

    @Test
    fun testTotalSpending_andPaymentMethodTotals() {
        val expenses = listOf(
            ExpenseWithCategory(
                expense = Expense(id = "1", amountPaise = 25000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = 1727424000000L),
                category = foodCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "2", amountPaise = 15000L, categoryId = "cat_food", paymentMethod = PaymentMethod.CASH, timestamp = 1727424000000L),
                category = foodCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "3", amountPaise = 60000L, categoryId = "cat_transport", paymentMethod = PaymentMethod.UPI, timestamp = 1727424000000L),
                category = transportCategory
            )
        )

        val result = analyticsUseCase.calculateAnalytics(
            period = AnalyticsPeriod.TODAY,
            startMillis = 1727424000000L,
            endMillis = 1727510399000L,
            expenses = expenses
        )

        // Total: 250 + 150 + 600 = ₹1,000 (100,000 paise)
        assertEquals(100000L, result.totalSpendingPaise)

        // UPI: 250 + 600 = ₹850 (85,000 paise, 85%)
        assertEquals(85000L, result.upiSpendingPaise)
        assertEquals(85.0f, result.upiPercentage, 0.01f)

        // Cash: ₹150 (15,000 paise, 15%)
        assertEquals(15000L, result.cashSpendingPaise)
        assertEquals(15.0f, result.cashPercentage, 0.01f)
    }

    @Test
    fun testCategoryTotals_andPercentages_sortedDescending() {
        val expenses = listOf(
            ExpenseWithCategory(
                expense = Expense(id = "1", amountPaise = 20000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = 1727424000000L),
                category = foodCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "2", amountPaise = 50000L, categoryId = "cat_transport", paymentMethod = PaymentMethod.UPI, timestamp = 1727424000000L),
                category = transportCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "3", amountPaise = 30000L, categoryId = "cat_bills", paymentMethod = PaymentMethod.CASH, timestamp = 1727424000000L),
                category = billsCategory
            )
        )

        val result = analyticsUseCase.calculateAnalytics(
            period = AnalyticsPeriod.THIS_MONTH,
            startMillis = 1727424000000L,
            endMillis = 1727510399000L,
            expenses = expenses
        )

        assertEquals(3, result.categorySpends.size)
        // Transport (50,000 paise, 50%)
        assertEquals("cat_transport", result.categorySpends[0].category?.id)
        assertEquals(50000L, result.categorySpends[0].amountPaise)
        assertEquals(50.0f, result.categorySpends[0].percentage, 0.01f)

        // Bills (30,000 paise, 30%)
        assertEquals("cat_bills", result.categorySpends[1].category?.id)
        assertEquals(30000L, result.categorySpends[1].amountPaise)
        assertEquals(30.0f, result.categorySpends[1].percentage, 0.01f)

        // Food (20,000 paise, 20%)
        assertEquals("cat_food", result.categorySpends[2].category?.id)
        assertEquals(20000L, result.categorySpends[2].amountPaise)
        assertEquals(20.0f, result.categorySpends[2].percentage, 0.01f)
    }

    @Test
    fun testLargestExpense_andHighestSpendingDay() {
        val date1 = LocalDate.of(2026, 9, 25)
        val date2 = LocalDate.of(2026, 9, 26)
        val date3 = LocalDate.of(2026, 9, 27)

        val zone = ZoneId.systemDefault()
        val t1 = date1.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val t2 = date2.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val t3 = date3.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()

        val expenses = listOf(
            ExpenseWithCategory(
                expense = Expense(id = "1", amountPaise = 10000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = t1),
                category = foodCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "2", amountPaise = 85000L, categoryId = "cat_bills", paymentMethod = PaymentMethod.UPI, timestamp = t2), // Largest single
                category = billsCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "3", amountPaise = 50000L, categoryId = "cat_transport", paymentMethod = PaymentMethod.CASH, timestamp = t3),
                category = transportCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "4", amountPaise = 50000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = t3),
                category = foodCategory
            )
            // Day 1: 10000, Day 2: 85000, Day 3: 100000 (highest spending day)
        )

        val result = analyticsUseCase.calculateAnalytics(
            period = AnalyticsPeriod.THIS_WEEK,
            startMillis = DateTimeUtils.getDayStartEpochMillis(date1),
            endMillis = DateTimeUtils.getDayEndEpochMillis(date3),
            expenses = expenses
        )

        // Largest expense should be exp #2 (85,000 paise)
        assertNotNull(result.largestExpense)
        assertEquals("2", result.largestExpense?.expense?.id)
        assertEquals(85000L, result.largestExpense?.expense?.amountPaise)

        // Highest spending day should be date3 (100,000 paise)
        assertNotNull(result.highestSpendingDay)
        assertEquals(date3, result.highestSpendingDay?.first)
        assertEquals(100000L, result.highestSpendingDay?.second)
    }

    @Test
    fun testHourlyBreakdown_mapsAll24Hours() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val t8am = today.atTime(8, 30).atZone(zone).toInstant().toEpochMilli()
        val t2pm = today.atTime(14, 15).atZone(zone).toInstant().toEpochMilli()

        val expenses = listOf(
            ExpenseWithCategory(
                expense = Expense(id = "1", amountPaise = 12000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = t8am),
                category = foodCategory
            ),
            ExpenseWithCategory(
                expense = Expense(id = "2", amountPaise = 45000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = t2pm),
                category = foodCategory
            )
        )

        val result = analyticsUseCase.calculateAnalytics(
            period = AnalyticsPeriod.TODAY,
            startMillis = DateTimeUtils.getDayStartEpochMillis(today),
            endMillis = DateTimeUtils.getDayEndEpochMillis(today),
            expenses = expenses
        )

        assertEquals(24, result.hourlyBreakdown.size)
        // Hour 8 (8 AM)
        assertEquals(12000L, result.hourlyBreakdown[8].totalPaise)
        assertEquals("8 AM", result.hourlyBreakdown[8].label)

        // Hour 14 (2 PM)
        assertEquals(45000L, result.hourlyBreakdown[14].totalPaise)
        assertEquals("2 PM", result.hourlyBreakdown[14].label)

        // Hour 0 (12 AM) should be 0
        assertEquals(0L, result.hourlyBreakdown[0].totalPaise)
    }

    @Test
    fun testEmptyExpenses_returnsZeroMetricsSafelyWithoutExceptions() {
        val result = analyticsUseCase.calculateAnalytics(
            period = AnalyticsPeriod.THIS_MONTH,
            startMillis = 100000L,
            endMillis = 200000L,
            expenses = emptyList()
        )

        assertEquals(0L, result.totalSpendingPaise)
        assertEquals(0L, result.averageDailySpendingPaise)
        assertEquals(0L, result.upiSpendingPaise)
        assertEquals(0L, result.cashSpendingPaise)
        assertEquals(0f, result.upiPercentage, 0.001f)
        assertEquals(0f, result.cashPercentage, 0.001f)
        assertTrue(result.categorySpends.isEmpty())
        assertTrue(result.dailyBreakdown.isEmpty())
        assertTrue(result.hourlyBreakdown.isEmpty())
    }

    @Test
    fun testFlowExecution_withDateRange() = runBlocking {
        fakeExpenseRepo.expensesToReturn = listOf(
            ExpenseWithCategory(
                expense = Expense(id = "1", amountPaise = 50000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, timestamp = System.currentTimeMillis()),
                category = foodCategory
            )
        )

        val flow = analyticsUseCase(AnalyticsPeriod.TODAY)
        val data = flow.first()
        assertEquals(50000L, data.totalSpendingPaise)
        assertEquals(AnalyticsPeriod.TODAY, data.period)
    }

    private class FakeAnalyticsExpenseRepository : ExpenseRepository {
        var expensesToReturn = listOf<ExpenseWithCategory>()

        override fun getAllExpenses(): Flow<List<ExpenseWithCategory>> = flowOf(expensesToReturn)
        override fun getExpensesForDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>> = flowOf(expensesToReturn)
        override fun getTodayExpenses(): Flow<List<ExpenseWithCategory>> = flowOf(expensesToReturn)
        override suspend fun getExpenseById(id: String): ExpenseWithCategory? = null
        override fun getTodayTotalPaise(): Flow<Long> = flowOf(0L)
        override fun getTodayUpiTotalPaise(): Flow<Long> = flowOf(0L)
        override fun getTodayCashTotalPaise(): Flow<Long> = flowOf(0L)
        override fun getTotalForDateRange(startMillis: Long, endMillis: Long): Flow<Long> = flowOf(0L)
        override fun getCategoryTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpendAggregate>> = flowOf(emptyList())
        override fun getPaymentMethodTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<PaymentMethodSpendAggregate>> = flowOf(emptyList())
        override suspend fun getLargestExpenseForDateRange(startMillis: Long, endMillis: Long): ExpenseWithCategory? = null
        override fun searchExpenses(query: String): Flow<List<ExpenseWithCategory>> = flowOf(emptyList())
        override suspend fun addExpense(expense: Expense) {}
        override suspend fun updateExpense(expense: Expense) {}
        override suspend fun deleteExpense(id: String) {}
        override suspend fun restoreExpense(expense: Expense) {}
        override suspend fun clearAllExpenses() {}
        override suspend fun getAllExpensesSync(): List<Expense> = emptyList()
        override suspend fun restoreBackupExpenses(expenses: List<Expense>) {}
        override suspend fun findRecentSimilarExpense(
            categoryId: String,
            amountPaise: Long,
            paymentMethod: com.hrshd1eux.expensetracker.domain.model.PaymentMethod,
            sinceEpochMillis: Long
        ): Expense? = null

        override fun getCategoryUsageCounts(): Flow<Map<String, Int>> = flowOf(emptyMap())
    }
}
