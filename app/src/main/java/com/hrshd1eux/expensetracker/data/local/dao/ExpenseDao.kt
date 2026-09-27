package com.hrshd1eux.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hrshd1eux.expensetracker.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class CategorySpendAggregate(
    val categoryId: String,
    val totalPaise: Long
)

data class PaymentMethodSpendAggregate(
    val paymentMethod: String,
    val totalPaise: Long
)

data class CategoryUsageAggregate(
    val categoryId: String,
    val count: Int
)

@Dao
interface ExpenseDao {

    @Query("SELECT categoryId, COUNT(*) as count FROM expenses GROUP BY categoryId ORDER BY count DESC")
    fun getCategoryUsageCounts(): Flow<List<CategoryUsageAggregate>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseByIdFlow(id: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp DESC")
    fun getExpensesForDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp DESC")
    suspend fun getExpensesForDateRangeSync(startMillis: Long, endMillis: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis")
    fun getTotalForDateRange(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis")
    suspend fun getTotalForDateRangeSync(startMillis: Long, endMillis: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis AND paymentMethod = :paymentMethod")
    fun getTotalByPaymentMethodForDateRange(startMillis: Long, endMillis: Long, paymentMethod: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis AND paymentMethod = :paymentMethod")
    suspend fun getTotalByPaymentMethodForDateRangeSync(startMillis: Long, endMillis: Long, paymentMethod: String): Long

    @Query("SELECT categoryId, COALESCE(SUM(amountPaise), 0) as totalPaise FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis GROUP BY categoryId ORDER BY totalPaise DESC")
    fun getCategoryTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpendAggregate>>

    @Query("SELECT paymentMethod, COALESCE(SUM(amountPaise), 0) as totalPaise FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis GROUP BY paymentMethod")
    fun getPaymentMethodTotalsForDateRange(startMillis: Long, endMillis: Long): Flow<List<PaymentMethodSpendAggregate>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY amountPaise DESC LIMIT 1")
    suspend fun getLargestExpenseForDateRange(startMillis: Long, endMillis: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE (note LIKE '%' || :query || '%' OR paymentMethod LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchExpenses(query: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun countExpensesForCategory(categoryId: String): Int

    @Query("SELECT * FROM expenses WHERE timestamp >= :sinceEpochMillis AND categoryId = :categoryId AND amountPaise = :amountPaise AND paymentMethod = :paymentMethod LIMIT 1")
    suspend fun findRecentSimilarExpense(
        categoryId: String,
        amountPaise: Long,
        paymentMethod: String,
        sinceEpochMillis: Long
    ): ExpenseEntity?
}
