package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.data.local.dao.CategorySpendAggregate
import com.hrshd1eux.expensetracker.data.local.dao.PaymentMethodSpendAggregate
import com.hrshd1eux.expensetracker.data.preferences.UserPreferences
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.domain.usecase.AddExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.DeleteExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetHistoryExpensesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetTodaySummaryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.HistoryFilter
import com.hrshd1eux.expensetracker.domain.usecase.RestoreExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.UpdateExpenseUseCase
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseFlowIntegrationTest {

    private lateinit var fakeExpenseRepo: FakeFlowExpenseRepository
    private lateinit var fakeCategoryRepo: FakeFlowCategoryRepository
    private lateinit var fakeUserPrefs: FakeFlowUserPreferences

    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var restoreExpenseUseCase: RestoreExpenseUseCase
    private lateinit var getTodaySummaryUseCase: GetTodaySummaryUseCase
    private lateinit var getHistoryExpensesUseCase: GetHistoryExpensesUseCase

    private val foodCategory = Category(id = "cat_food", name = "Food", icon = "restaurant", color = 0xFFFB8C00, isDefault = true)
    private val transportCategory = Category(id = "cat_transport", name = "Transport", icon = "directions_transit", color = 0xFF1E88E5, isDefault = true)

    @Before
    fun setup() {
        fakeExpenseRepo = FakeFlowExpenseRepository()
        fakeCategoryRepo = FakeFlowCategoryRepository()
        fakeUserPrefs = FakeFlowUserPreferences()

        fakeCategoryRepo.categories.add(foodCategory)
        fakeCategoryRepo.categories.add(transportCategory)

        fakeExpenseRepo.categoryRepo = fakeCategoryRepo

        addExpenseUseCase = AddExpenseUseCase(fakeExpenseRepo, fakeUserPrefs)
        updateExpenseUseCase = UpdateExpenseUseCase(fakeExpenseRepo)
        deleteExpenseUseCase = DeleteExpenseUseCase(fakeExpenseRepo)
        restoreExpenseUseCase = RestoreExpenseUseCase(fakeExpenseRepo)
        getTodaySummaryUseCase = GetTodaySummaryUseCase(fakeExpenseRepo)
        getHistoryExpensesUseCase = GetHistoryExpensesUseCase(fakeExpenseRepo)
    }

    @Test
    fun testAddExpense_save_homeUpdates() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Initial today summary is 0
        val initialSummary = getTodaySummaryUseCase().first()
        assertEquals(0L, initialSummary.totalPaise)
        assertEquals(0L, initialSummary.upiTotalPaise)
        assertEquals(0L, initialSummary.cashTotalPaise)
        assertTrue(initialSummary.expenses.isEmpty())

        // 2. Add an expense: ₹250 (25,000 paise) via UPI
        val expense = Expense(
            id = "exp_1",
            amountPaise = 25000L,
            categoryId = "cat_food",
            paymentMethod = PaymentMethod.UPI,
            note = "Lunch at cafe",
            timestamp = now
        )
        val result = addExpenseUseCase(expense)
        assertTrue(result.isSuccess)

        // 3. Today's summary on Home updates
        val updatedSummary = getTodaySummaryUseCase().first()
        assertEquals(25000L, updatedSummary.totalPaise)
        assertEquals(25000L, updatedSummary.upiTotalPaise)
        assertEquals(0L, updatedSummary.cashTotalPaise)
        assertEquals(1, updatedSummary.expenses.size)
        assertEquals("Lunch at cafe", updatedSummary.expenses.first().expense.note)
    }

    @Test
    fun testAdd_edit_save() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Add expense
        val original = Expense(
            id = "exp_edit",
            amountPaise = 15000L,
            categoryId = "cat_food",
            paymentMethod = PaymentMethod.UPI,
            note = "Coffee",
            timestamp = now
        )
        addExpenseUseCase(original)
        assertEquals(15000L, getTodaySummaryUseCase().first().totalPaise)

        // 2. Edit expense: change amount to ₹200 (20,000 paise), category to Transport, and payment to CASH
        val modified = original.copy(
            amountPaise = 20000L,
            categoryId = "cat_transport",
            paymentMethod = PaymentMethod.CASH,
            note = "Coffee & Taxi"
        )
        updateExpenseUseCase(modified)

        // 3. Verify updated state
        val updatedSummary = getTodaySummaryUseCase().first()
        assertEquals(20000L, updatedSummary.totalPaise)
        assertEquals(20000L, updatedSummary.cashTotalPaise)
        assertEquals(0L, updatedSummary.upiTotalPaise)

        val retrieved = fakeExpenseRepo.getExpenseById(original.id)
        assertNotNull(retrieved)
        assertEquals("Coffee & Taxi", retrieved?.expense?.note)
        assertEquals(PaymentMethod.CASH, retrieved?.expense?.paymentMethod)
        assertEquals("cat_transport", retrieved?.expense?.categoryId)
    }

    @Test
    fun testAdd_delete_undo() = runBlocking {
        val now = System.currentTimeMillis()

        val expense = Expense(
            id = "exp_del",
            amountPaise = 50000L,
            categoryId = "cat_food",
            paymentMethod = PaymentMethod.UPI,
            note = "Dinner party",
            timestamp = now
        )
        addExpenseUseCase(expense)
        assertEquals(1, fakeExpenseRepo.expensesList.value.size)

        // Delete
        deleteExpenseUseCase(expense.id)
        assertEquals(0, fakeExpenseRepo.expensesList.value.size)
        assertEquals(0L, getTodaySummaryUseCase().first().totalPaise)

        // Undo (Restore)
        restoreExpenseUseCase(expense)
        assertEquals(1, fakeExpenseRepo.expensesList.value.size)
        assertEquals(50000L, getTodaySummaryUseCase().first().totalPaise)
        assertEquals(expense.id, fakeExpenseRepo.expensesList.value.first().id)
    }

    @Test
    fun testSearch_resultAppears() = runBlocking {
        val now = System.currentTimeMillis()

        addExpenseUseCase(Expense(id = "1", amountPaise = 25000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, note = "Lunch at Subway", timestamp = now))
        addExpenseUseCase(Expense(id = "2", amountPaise = 4000L, categoryId = "cat_transport", paymentMethod = PaymentMethod.CASH, note = "Bus ticket", timestamp = now))
        addExpenseUseCase(Expense(id = "3", amountPaise = 95000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, note = "Groceries", timestamp = now))

        // Search "subway"
        val filter1 = HistoryFilter(searchQuery = "subway")
        val groups1 = getHistoryExpensesUseCase(filter1).first()
        val allExpenses1 = groups1.flatMap { it.expenses }
        assertEquals(1, allExpenses1.size)
        assertEquals("Lunch at Subway", allExpenses1.first().expense.note)

        // Search "bus"
        val filter2 = HistoryFilter(searchQuery = "bus")
        val groups2 = getHistoryExpensesUseCase(filter2).first()
        val allExpenses2 = groups2.flatMap { it.expenses }
        assertEquals(1, allExpenses2.size)
        assertEquals("Bus ticket", allExpenses2.first().expense.note)

        // Search "40" (matches amount ₹40)
        val filter3 = HistoryFilter(searchQuery = "40")
        val groups3 = getHistoryExpensesUseCase(filter3).first()
        val allExpenses3 = groups3.flatMap { it.expenses }
        assertEquals(1, allExpenses3.size)
        assertEquals(4000L, allExpenses3.first().expense.amountPaise)
    }

    @Test
    fun testFilter_correctTransactionsAppear() = runBlocking {
        val now = System.currentTimeMillis()

        addExpenseUseCase(Expense(id = "1", amountPaise = 10000L, categoryId = "cat_food", paymentMethod = PaymentMethod.UPI, note = "Tea", timestamp = now))
        addExpenseUseCase(Expense(id = "2", amountPaise = 20000L, categoryId = "cat_food", paymentMethod = PaymentMethod.CASH, note = "Snacks", timestamp = now))
        addExpenseUseCase(Expense(id = "3", amountPaise = 30000L, categoryId = "cat_transport", paymentMethod = PaymentMethod.UPI, note = "Cab", timestamp = now))
        addExpenseUseCase(Expense(id = "4", amountPaise = 80000L, categoryId = "cat_transport", paymentMethod = PaymentMethod.CASH, note = "Train ticket", timestamp = now))

        // Filter: PaymentMethod.CASH only
        val cashFilter = HistoryFilter(paymentMethod = PaymentMethod.CASH)
        val cashGroups = getHistoryExpensesUseCase(cashFilter).first()
        val cashItems = cashGroups.flatMap { it.expenses }
        assertEquals(2, cashItems.size)
        assertTrue(cashItems.all { it.expense.paymentMethod == PaymentMethod.CASH })

        // Filter: Category Food only
        val foodFilter = HistoryFilter(selectedCategoryIds = setOf("cat_food"))
        val foodGroups = getHistoryExpensesUseCase(foodFilter).first()
        val foodItems = foodGroups.flatMap { it.expenses }
        assertEquals(2, foodItems.size)
        assertTrue(foodItems.all { it.expense.categoryId == "cat_food" })

        // Filter: Amount min 25000 paise (₹250)
        val amountFilter = HistoryFilter(minAmountPaise = 25000L)
        val amountGroups = getHistoryExpensesUseCase(amountFilter).first()
        val amountItems = amountGroups.flatMap { it.expenses }
        assertEquals(2, amountItems.size)
        assertTrue(amountItems.all { it.expense.amountPaise >= 25000L })
    }

    // --- Fake Test Doubles with Flow emission ---

    private class FakeFlowUserPreferences : UserPreferences {
        override val currencyCodeFlow: Flow<String> = flowOf("INR")
        override val currencySymbolFlow: Flow<String> = flowOf("₹")
        override suspend fun setCurrency(code: String, symbol: String) {}
        override suspend fun setLastPaymentMethod(method: String) {}
        override suspend fun addRecentCategoryId(categoryId: String) {}
    }

    private class FakeFlowCategoryRepository : CategoryRepository {
        val categories = mutableListOf<Category>()
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override fun getActiveCategories(): Flow<List<Category>> = flowOf(categories.filter { !it.isArchived })
        override suspend fun getCategoryById(id: String): Category? = categories.find { it.id == id }
        override fun getCategoryByIdFlow(id: String): Flow<Category?> = flowOf(categories.find { it.id == id })
        override suspend fun addCategory(category: Category) { categories.add(category) }
        override suspend fun updateCategory(category: Category) {}
        override suspend fun archiveCategory(id: String, isArchived: Boolean) {}
        override suspend fun deleteCategory(id: String): Boolean = true
        override suspend fun prepopulateDefaultCategories() {}
    }

    private class FakeFlowExpenseRepository : ExpenseRepository {
        var categoryRepo: FakeFlowCategoryRepository? = null
        val expensesList = MutableStateFlow<List<Expense>>(emptyList())

        private fun toExpenseWithCategory(e: Expense): ExpenseWithCategory {
            val cat = categoryRepo?.categories?.find { it.id == e.categoryId }
            return ExpenseWithCategory(e, cat)
        }

        override fun getAllExpenses(): Flow<List<ExpenseWithCategory>> =
            expensesList.map { list -> list.map { toExpenseWithCategory(it) } }

        override fun getExpensesForDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>> =
            expensesList.map { list ->
                list.filter { it.timestamp in startMillis..endMillis }.map { toExpenseWithCategory(it) }
            }

        override fun getTodayExpenses(): Flow<List<ExpenseWithCategory>> {
            val start = DateTimeUtils.getDayStartEpochMillis()
            val end = DateTimeUtils.getDayEndEpochMillis()
            return getExpensesForDateRange(start, end)
        }

        override suspend fun getExpenseById(id: String): ExpenseWithCategory? {
            val exp = expensesList.value.find { it.id == id } ?: return null
            return toExpenseWithCategory(exp)
        }

        override fun getTodayTotalPaise(): Flow<Long> = getTodayExpenses().map { list ->
            list.sumOf { it.expense.amountPaise }
        }

        override fun getTodayUpiTotalPaise(): Flow<Long> = getTodayExpenses().map { list ->
            list.filter { it.expense.paymentMethod == PaymentMethod.UPI }.sumOf { it.expense.amountPaise }
        }

        override fun getTodayCashTotalPaise(): Flow<Long> = getTodayExpenses().map { list ->
            list.filter { it.expense.paymentMethod == PaymentMethod.CASH }.sumOf { it.expense.amountPaise }
        }

        override fun getTotalForDateRange(startMillis: Long, endMillis: Long): Flow<Long> =
            getExpensesForDateRange(startMillis, endMillis).map { list -> list.sumOf { it.expense.amountPaise } }

        override fun getCategoryTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpendAggregate>> =
            flowOf(emptyList())

        override fun getPaymentMethodTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<PaymentMethodSpendAggregate>> =
            flowOf(emptyList())

        override suspend fun getLargestExpenseForDateRange(startMillis: Long, endMillis: Long): ExpenseWithCategory? = null

        override fun searchExpenses(query: String): Flow<List<ExpenseWithCategory>> = getAllExpenses()

        override suspend fun addExpense(expense: Expense) {
            expensesList.value = listOf(expense) + expensesList.value
        }

        override suspend fun updateExpense(expense: Expense) {
            expensesList.value = expensesList.value.map { if (it.id == expense.id) expense else it }
        }

        override suspend fun deleteExpense(id: String) {
            expensesList.value = expensesList.value.filter { it.id != id }
        }

        override suspend fun restoreExpense(expense: Expense) {
            expensesList.value = listOf(expense) + expensesList.value
        }

        override suspend fun clearAllExpenses() {
            expensesList.value = emptyList()
        }

        override suspend fun getAllExpensesSync(): List<Expense> = expensesList.value

        override suspend fun restoreBackupExpenses(expenses: List<Expense>) {
            expensesList.value = expenses
        }

        override suspend fun findRecentSimilarExpense(
            categoryId: String,
            amountPaise: Long,
            paymentMethod: PaymentMethod,
            sinceEpochMillis: Long
        ): Expense? {
            return expensesList.value.find {
                it.categoryId == categoryId &&
                    it.amountPaise == amountPaise &&
                    it.paymentMethod == paymentMethod &&
                    it.timestamp >= sinceEpochMillis
            }
        }

        override fun getCategoryUsageCounts(): Flow<Map<String, Int>> {
            return expensesList.map { list ->
                list.groupingBy { it.categoryId }.eachCount()
            }
        }
    }
}
