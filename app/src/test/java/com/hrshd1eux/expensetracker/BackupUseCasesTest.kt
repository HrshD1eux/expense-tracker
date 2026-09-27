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
import com.hrshd1eux.expensetracker.domain.usecase.ExportBackupUseCase
import com.hrshd1eux.expensetracker.domain.usecase.ImportBackupUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupUseCasesTest {

    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeUserPrefs: FakeUserPreferences
    private lateinit var exportUseCase: ExportBackupUseCase
    private lateinit var importUseCase: ImportBackupUseCase

    @Before
    fun setup() {
        fakeExpenseRepo = FakeExpenseRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeUserPrefs = FakeUserPreferences()

        exportUseCase = ExportBackupUseCase(fakeExpenseRepo, fakeCategoryRepo, fakeUserPrefs)
        importUseCase = ImportBackupUseCase(fakeExpenseRepo, fakeCategoryRepo, fakeUserPrefs)

        // Seed some test data
        fakeCategoryRepo.categories.add(
            Category(id = "cat_food", name = "Food", icon = "restaurant", color = 0xFFFB8C00, isDefault = true)
        )
        fakeCategoryRepo.categories.add(
            Category(id = "cat_transport", name = "Transport", icon = "directions_transit", color = 0xFF1E88E5, isDefault = true)
        )

        fakeExpenseRepo.expenses.add(
            Expense(
                id = "exp_1",
                amountPaise = 25000L,
                categoryId = "cat_food",
                paymentMethod = PaymentMethod.UPI,
                note = "Lunch",
                timestamp = 1727424000000L
            )
        )
        fakeExpenseRepo.expenses.add(
            Expense(
                id = "exp_2",
                amountPaise = 4000L,
                categoryId = "cat_transport",
                paymentMethod = PaymentMethod.CASH,
                note = "Bus ticket",
                timestamp = 1727430000000L
            )
        )
    }

    @Test
    fun testExportPlaintextBackup_matchesVersionedFormat() = runBlocking {
        val result = exportUseCase()
        assertTrue(result.isSuccess)
        val jsonStr = result.getOrThrow()

        val json = JSONObject(jsonStr)
        assertEquals(1, json.getInt("backupVersion"))
        assertEquals("INR", json.getString("currency"))
        assertTrue(json.has("createdAt"))

        val categories = json.getJSONArray("categories")
        assertEquals(2, categories.length())
        assertEquals("cat_food", categories.getJSONObject(0).getString("id"))

        val expenses = json.getJSONArray("expenses")
        assertEquals(2, expenses.length())
        assertEquals(25000L, expenses.getJSONObject(0).getLong("amountPaise"))
        assertEquals("UPI", expenses.getJSONObject(0).getString("paymentMethod"))
        assertEquals("Lunch", expenses.getJSONObject(0).getString("note"))
    }

    @Test
    fun testExportAndImportEncryptedBackup_withCorrectPassword() = runBlocking {
        val password = "SuperSecurePassword123!"
        val exportResult = exportUseCase(password)
        assertTrue(exportResult.isSuccess)

        val encryptedJson = exportResult.getOrThrow()
        val env = JSONObject(encryptedJson)
        assertEquals("encrypted_backup", env.getString("type"))
        assertTrue(env.has("salt"))
        assertTrue(env.has("iv"))
        assertTrue(env.has("ciphertext"))

        // Clear repo data to simulate fresh install
        fakeCategoryRepo.categories.clear()
        fakeExpenseRepo.expenses.clear()

        // Import with correct password
        val importResult = importUseCase(encryptedJson, password)
        assertTrue("Import should succeed", importResult is ImportBackupUseCase.ImportResult.Success)
        val success = importResult as ImportBackupUseCase.ImportResult.Success
        assertEquals(2, success.categoryCount)
        assertEquals(2, success.expenseCount)

        assertEquals(2, fakeExpenseRepo.expenses.size)
        assertEquals(25000L, fakeExpenseRepo.expenses[0].amountPaise)
    }

    @Test
    fun testImportEncryptedBackup_withoutPassword_returnsPasswordRequired() = runBlocking {
        val exportResult = exportUseCase("password123")
        val encryptedJson = exportResult.getOrThrow()

        val importResult = importUseCase(encryptedJson, password = null)
        assertTrue(importResult is ImportBackupUseCase.ImportResult.PasswordRequired)
    }

    @Test
    fun testImportEncryptedBackup_withWrongPassword_returnsError() = runBlocking {
        val exportResult = exportUseCase("CorrectPassword123")
        val encryptedJson = exportResult.getOrThrow()

        val importResult = importUseCase(encryptedJson, password = "WrongPassword999")
        assertTrue(importResult is ImportBackupUseCase.ImportResult.Error)
        val err = importResult as ImportBackupUseCase.ImportResult.Error
        assertTrue(err.message.contains("Incorrect password") || err.message.contains("corrupted"))
    }

    @Test
    fun testImportMalformedJson_handlesGracefullyWithoutCrashing() = runBlocking {
        // Empty string
        val res1 = importUseCase("")
        assertTrue(res1 is ImportBackupUseCase.ImportResult.Error)

        // Invalid JSON
        val res2 = importUseCase("Not a JSON document {{{")
        assertTrue(res2 is ImportBackupUseCase.ImportResult.Error)

        // Missing backupVersion
        val res3 = importUseCase("""{"currency":"INR","categories":[],"expenses":[]}""")
        assertTrue(res3 is ImportBackupUseCase.ImportResult.Error)

        // Unsupported version
        val res4 = importUseCase("""{"backupVersion":99,"categories":[],"expenses":[]}""")
        assertTrue(res4 is ImportBackupUseCase.ImportResult.Error)
        assertTrue((res4 as ImportBackupUseCase.ImportResult.Error).message.contains("Unsupported backup version"))

        // Missing categories
        val res5 = importUseCase("""{"backupVersion":1,"expenses":[]}""")
        assertTrue(res5 is ImportBackupUseCase.ImportResult.Error)

        // Missing expenses
        val res6 = importUseCase("""{"backupVersion":1,"categories":[]}""")
        assertTrue(res6 is ImportBackupUseCase.ImportResult.Error)
    }

    @Test
    fun testImportDataIntegrity_invalidAmountsSkipped_unknownCategoryMappedToOther() = runBlocking {
        fakeExpenseRepo.expenses.clear()
        fakeCategoryRepo.categories.clear()

        val backupJson = """
            {
              "backupVersion": 1,
              "currency": "USD",
              "categories": [
                {"id": "cat_food", "name": "Food", "color": -292864, "icon": "restaurant"}
              ],
              "expenses": [
                {
                  "id": "e1",
                  "amountPaise": 50000,
                  "categoryId": "cat_food",
                  "paymentMethod": "UPI",
                  "note": "Valid expense",
                  "timestamp": 1727424000000
                },
                {
                  "id": "e2_invalid_amount",
                  "amountPaise": 0,
                  "categoryId": "cat_food",
                  "paymentMethod": "CASH",
                  "timestamp": 1727424000000
                },
                {
                  "id": "e3_negative_amount",
                  "amountPaise": -500,
                  "categoryId": "cat_food",
                  "paymentMethod": "UPI",
                  "timestamp": 1727424000000
                },
                {
                  "id": "e4_unknown_cat",
                  "amountPaise": 15000,
                  "categoryId": "cat_non_existent",
                  "paymentMethod": "UNKNOWN_METHOD",
                  "timestamp": 1727424000000
                }
              ]
            }
        """.trimIndent()

        val result = importUseCase(backupJson)
        assertTrue(result is ImportBackupUseCase.ImportResult.Success)
        val success = result as ImportBackupUseCase.ImportResult.Success
        assertEquals(1, success.categoryCount)
        assertEquals(2, success.expenseCount) // Only e1 and e4 are valid; e2 and e3 skipped due to non-positive amount

        // Check currency was restored
        assertEquals("USD", fakeUserPrefs.storedCurrencyCode)
        assertEquals("$", fakeUserPrefs.storedCurrencySymbol)

        // Check data normalization
        val e4 = fakeExpenseRepo.expenses.first { it.id == "e4_unknown_cat" }
        assertEquals("cat_other", e4.categoryId) // mapped to cat_other fallback
        assertEquals(PaymentMethod.UPI, e4.paymentMethod) // defaulted to UPI for unknown method
    }

    // --- Test Doubles ---

    private class FakeUserPreferences : UserPreferences {
        var storedCurrencyCode = "INR"
        var storedCurrencySymbol = "₹"

        override val currencyCodeFlow: Flow<String> = flowOf(storedCurrencyCode)
        override val currencySymbolFlow: Flow<String> = flowOf(storedCurrencySymbol)

        override suspend fun setCurrency(code: String, symbol: String) {
            storedCurrencyCode = code
            storedCurrencySymbol = symbol
        }

        override suspend fun setLastPaymentMethod(method: String) {}
        override suspend fun addRecentCategoryId(categoryId: String) {}
    }

    private class FakeExpenseRepository : ExpenseRepository {
        val expenses = mutableListOf<Expense>()

        override fun getAllExpenses(): Flow<List<ExpenseWithCategory>> = flowOf(emptyList())
        override fun getExpensesForDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>> = flowOf(emptyList())
        override fun getTodayExpenses(): Flow<List<ExpenseWithCategory>> = flowOf(emptyList())
        override suspend fun getExpenseById(id: String): ExpenseWithCategory? = null
        override fun getTodayTotalPaise(): Flow<Long> = flowOf(0L)
        override fun getTodayUpiTotalPaise(): Flow<Long> = flowOf(0L)
        override fun getTodayCashTotalPaise(): Flow<Long> = flowOf(0L)
        override fun getTotalForDateRange(startMillis: Long, endMillis: Long): Flow<Long> = flowOf(0L)
        override fun getCategoryTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpendAggregate>> = flowOf(emptyList())
        override fun getPaymentMethodTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<PaymentMethodSpendAggregate>> = flowOf(emptyList())
        override suspend fun getLargestExpenseForDateRange(startMillis: Long, endMillis: Long): ExpenseWithCategory? = null
        override fun searchExpenses(query: String): Flow<List<ExpenseWithCategory>> = flowOf(emptyList())
        override suspend fun addExpense(expense: Expense) { expenses.add(expense) }
        override suspend fun updateExpense(expense: Expense) {
            val idx = expenses.indexOfFirst { it.id == expense.id }
            if (idx != -1) expenses[idx] = expense
        }
        override suspend fun deleteExpense(id: String) { expenses.removeAll { it.id == id } }
        override suspend fun restoreExpense(expense: Expense) { expenses.add(expense) }
        override suspend fun clearAllExpenses() { expenses.clear() }
        override suspend fun getAllExpensesSync(): List<Expense> = expenses.toList()
        override suspend fun restoreBackupExpenses(expenses: List<Expense>) {
            this.expenses.addAll(expenses)
        }
        override suspend fun findRecentSimilarExpense(
            categoryId: String,
            amountPaise: Long,
            paymentMethod: com.hrshd1eux.expensetracker.domain.model.PaymentMethod,
            sinceEpochMillis: Long
        ): Expense? = null

        override fun getCategoryUsageCounts(): Flow<Map<String, Int>> = flowOf(emptyMap())
    }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = mutableListOf<Category>()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.toList())
        override fun getActiveCategories(): Flow<List<Category>> = flowOf(categories.filter { !it.isArchived })
        override suspend fun getCategoryById(id: String): Category? = categories.find { it.id == id }
        override fun getCategoryByIdFlow(id: String): Flow<Category?> = flowOf(categories.find { it.id == id })
        override suspend fun addCategory(category: Category) {
            val idx = categories.indexOfFirst { it.id == category.id }
            if (idx != -1) categories[idx] = category else categories.add(category)
        }
        override suspend fun updateCategory(category: Category) { addCategory(category) }
        override suspend fun archiveCategory(id: String, isArchived: Boolean) {
            val cat = categories.find { it.id == id }
            if (cat != null) addCategory(cat.copy(isArchived = isArchived))
        }
        override suspend fun deleteCategory(id: String): Boolean {
            return categories.removeAll { it.id == id }
        }
        override suspend fun prepopulateDefaultCategories() {}
    }
}
