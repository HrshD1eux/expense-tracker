package com.hrshd1eux.expensetracker.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.hrshd1eux.expensetracker.data.local.dao.CategoryDao
import com.hrshd1eux.expensetracker.data.local.dao.CategorySpendAggregate
import com.hrshd1eux.expensetracker.data.local.dao.ExpenseDao
import com.hrshd1eux.expensetracker.data.local.dao.PaymentMethodSpendAggregate
import com.hrshd1eux.expensetracker.data.local.entity.toDomain
import com.hrshd1eux.expensetracker.data.local.entity.toEntity
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import com.hrshd1eux.expensetracker.widget.ExpenseWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    @ApplicationContext private val context: Context
) : ExpenseRepository {

    private fun combineWithCategories(
        expenseFlow: Flow<List<com.hrshd1eux.expensetracker.data.local.entity.ExpenseEntity>>
    ): Flow<List<ExpenseWithCategory>> {
        return combine(
            expenseFlow,
            categoryDao.getAllCategories()
        ) { expenseEntities, categoryEntities ->
            val categoryMap = categoryEntities.associateBy({ it.id }, { it.toDomain() })
            expenseEntities.map { entity ->
                val expense = entity.toDomain()
                ExpenseWithCategory(
                    expense = expense,
                    category = categoryMap[expense.categoryId]
                )
            }
        }
    }

    override fun getAllExpenses(): Flow<List<ExpenseWithCategory>> {
        return combineWithCategories(expenseDao.getAllExpenses())
    }

    override fun getExpensesForDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<ExpenseWithCategory>> {
        return combineWithCategories(expenseDao.getExpensesForDateRange(startMillis, endMillis))
    }

    override fun getTodayExpenses(): Flow<List<ExpenseWithCategory>> {
        val start = DateTimeUtils.getDayStartEpochMillis()
        val end = DateTimeUtils.getDayEndEpochMillis()
        return getExpensesForDateRange(start, end)
    }

    override suspend fun getExpenseById(id: String): ExpenseWithCategory? {
        val entity = expenseDao.getExpenseById(id) ?: return null
        val category = categoryDao.getCategoryById(entity.categoryId)?.toDomain()
        return ExpenseWithCategory(expense = entity.toDomain(), category = category)
    }

    override fun getTodayTotalPaise(): Flow<Long> {
        val start = DateTimeUtils.getDayStartEpochMillis()
        val end = DateTimeUtils.getDayEndEpochMillis()
        return expenseDao.getTotalForDateRange(start, end)
    }

    override fun getTodayUpiTotalPaise(): Flow<Long> {
        val start = DateTimeUtils.getDayStartEpochMillis()
        val end = DateTimeUtils.getDayEndEpochMillis()
        return expenseDao.getTotalByPaymentMethodForDateRange(start, end, PaymentMethod.UPI.name)
    }

    override fun getTodayCashTotalPaise(): Flow<Long> {
        val start = DateTimeUtils.getDayStartEpochMillis()
        val end = DateTimeUtils.getDayEndEpochMillis()
        return expenseDao.getTotalByPaymentMethodForDateRange(start, end, PaymentMethod.CASH.name)
    }

    override fun getTotalForDateRange(startMillis: Long, endMillis: Long): Flow<Long> {
        return expenseDao.getTotalForDateRange(startMillis, endMillis)
    }

    override fun getCategoryTotalsForDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<CategorySpendAggregate>> {
        return expenseDao.getCategoryTotalsForDateRange(startMillis, endMillis)
    }

    override fun getPaymentMethodTotalsForDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<PaymentMethodSpendAggregate>> {
        return expenseDao.getPaymentMethodTotalsForDateRange(startMillis, endMillis)
    }

    override suspend fun getLargestExpenseForDateRange(
        startMillis: Long,
        endMillis: Long
    ): ExpenseWithCategory? {
        val entity = expenseDao.getLargestExpenseForDateRange(startMillis, endMillis) ?: return null
        val category = categoryDao.getCategoryById(entity.categoryId)?.toDomain()
        return ExpenseWithCategory(expense = entity.toDomain(), category = category)
    }

    override fun searchExpenses(query: String): Flow<List<ExpenseWithCategory>> {
        return combine(
            expenseDao.getAllExpenses(),
            categoryDao.getAllCategories()
        ) { expenseEntities, categoryEntities ->
            val categoryMap = categoryEntities.associateBy({ it.id }, { it.toDomain() })
            val lowerQuery = query.trim().lowercase()

            expenseEntities
                .map { entity ->
                    val expense = entity.toDomain()
                    ExpenseWithCategory(
                        expense = expense,
                        category = categoryMap[expense.categoryId]
                    )
                }
                .filter { item ->
                    if (lowerQuery.isEmpty()) true
                    else {
                        val noteMatches = item.expense.note.lowercase().contains(lowerQuery)
                        val paymentMatches = item.expense.paymentMethod.displayName.lowercase().contains(lowerQuery)
                        val categoryMatches = item.category?.name?.lowercase()?.contains(lowerQuery) == true
                        val amountMatches = (item.expense.amountPaise / 100L).toString().contains(lowerQuery)
                        noteMatches || paymentMatches || categoryMatches || amountMatches
                    }
                }
        }
    }

    override suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
        notifyWidgetUpdate()
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toEntity())
        notifyWidgetUpdate()
    }

    override suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpenseById(id)
        notifyWidgetUpdate()
    }

    override suspend fun restoreExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
        notifyWidgetUpdate()
    }

    override suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
        notifyWidgetUpdate()
    }

    override suspend fun getAllExpensesSync(): List<Expense> {
        return expenseDao.getExpensesForDateRangeSync(0L, Long.MAX_VALUE).map { it.toDomain() }
    }

    override suspend fun restoreBackupExpenses(expenses: List<Expense>) {
        expenseDao.insertExpenses(expenses.map { it.toEntity() })
        notifyWidgetUpdate()
    }

    override suspend fun findRecentSimilarExpense(
        categoryId: String,
        amountPaise: Long,
        paymentMethod: PaymentMethod,
        sinceEpochMillis: Long
    ): Expense? {
        return expenseDao.findRecentSimilarExpense(
            categoryId = categoryId,
            amountPaise = amountPaise,
            paymentMethod = paymentMethod.name,
            sinceEpochMillis = sinceEpochMillis
        )?.toDomain()
    }

    override fun getCategoryUsageCounts(): Flow<Map<String, Int>> {
        return expenseDao.getCategoryUsageCounts().map { list ->
            list.associate { it.categoryId to it.count }
        }
    }

    private suspend fun notifyWidgetUpdate() {
        try {
            ExpenseWidget().updateAll(context)
        } catch (e: Exception) {
            // Safe fallback if widget is not active
        }
    }
}
