package com.hrshd1eux.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod

@Entity(
    tableName = "expenses",
    indices = [
        Index("timestamp"),
        Index("categoryId"),
        Index("paymentMethod")
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val amountPaise: Long,
    val categoryId: String,
    val paymentMethod: String,
    val note: String,
    val timestamp: Long,
    val isReimbursable: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amountPaise = amountPaise,
    categoryId = categoryId,
    paymentMethod = try {
        PaymentMethod.valueOf(paymentMethod)
    } catch (e: Exception) {
        PaymentMethod.UPI
    },
    note = note,
    timestamp = timestamp,
    isReimbursable = isReimbursable,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amountPaise = amountPaise,
    categoryId = categoryId,
    paymentMethod = paymentMethod.name,
    note = note,
    timestamp = timestamp,
    isReimbursable = isReimbursable,
    createdAt = createdAt,
    updatedAt = updatedAt
)
