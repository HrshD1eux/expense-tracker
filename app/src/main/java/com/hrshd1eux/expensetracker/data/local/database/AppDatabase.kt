package com.hrshd1eux.expensetracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hrshd1eux.expensetracker.data.local.dao.CategoryDao
import com.hrshd1eux.expensetracker.data.local.dao.ExpenseDao
import com.hrshd1eux.expensetracker.data.local.dao.RecurringExpenseDao
import com.hrshd1eux.expensetracker.data.local.entity.CategoryEntity
import com.hrshd1eux.expensetracker.data.local.entity.ExpenseEntity
import com.hrshd1eux.expensetracker.data.local.entity.RecurringExpenseEntity

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        RecurringExpenseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"
    }
}
