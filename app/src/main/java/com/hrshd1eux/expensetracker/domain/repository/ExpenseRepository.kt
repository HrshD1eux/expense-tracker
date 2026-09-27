package com.hrshd1eux.expensetracker.domain.repository

import com.hrshd1eux.expensetracker.data.local.dao.CategorySpendAggregate
import com.hrshd1eux.expensetracker.data.local.dao.PaymentMethodSpendAggregate
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseWithCategory>>
    fun getExpensesForDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>
    fun getTodayExpenses(): Flow<List<ExpenseWithCategory>>
    suspend fun getExpenseById(id: String): ExpenseWithCategory?
    fun getTodayTotalPaise(): Flow<Long>
    fun getTodayUpiTotalPaise(): Flow<Long>
    fun getTodayCashTotalPaise(): Flow<Long>
    fun getTotalForDateRange(startMillis: Long, endMillis: Long): Flow<Long>
    fun getCategoryTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpendAggregate>>
    fun getPaymentMethodTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<PaymentMethodSpendAggregate>>
    suspend fun getLargestExpenseForDateRange(startMillis: Long, endMillis: Long): ExpenseWithCategory?
    fun searchExpenses(query: String): Flow<List<ExpenseWithCategory>>
    suspend fun addExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(id: String)
    suspend fun restoreExpense(expense: Expense)
    suspend fun clearAllExpenses()
    suspend fun getAllExpensesSync(): List<Expense>
    suspend fun restoreBackupExpenses(expenses: List<Expense>)
    suspend fun findRecentSimilarExpense(
        categoryId: String,
        amountPaise: Long,
        paymentMethod: PaymentMethod,
        sinceEpochMillis: Long
    ): Expense?
    fun getCategoryUsageCounts(): Flow<Map<String, Int>>
}
