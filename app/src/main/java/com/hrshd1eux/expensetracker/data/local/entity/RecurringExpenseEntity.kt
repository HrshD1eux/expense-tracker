package com.hrshd1eux.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.model.RecurringExpense
import com.hrshd1eux.expensetracker.domain.model.RecurringFrequency

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amountPaise: Long,
    val categoryId: String,
    val paymentMethod: String,
    val frequency: String,
    val nextDueDateEpochMillis: Long,
    val isActive: Boolean = true,
    val autoAdd: Boolean = true
)

fun RecurringExpenseEntity.toDomain(): RecurringExpense = RecurringExpense(
    id = id,
    title = title,
    amountPaise = amountPaise,
    categoryId = categoryId,
    paymentMethod = try {
        PaymentMethod.valueOf(paymentMethod)
    } catch (_: Exception) {
        PaymentMethod.UPI
    },
    frequency = try {
        RecurringFrequency.valueOf(frequency)
    } catch (_: Exception) {
        RecurringFrequency.MONTHLY
    },
    nextDueDateEpochMillis = nextDueDateEpochMillis,
    isActive = isActive,
    autoAdd = autoAdd
)

fun RecurringExpense.toEntity(): RecurringExpenseEntity = RecurringExpenseEntity(
    id = id,
    title = title,
    amountPaise = amountPaise,
    categoryId = categoryId,
    paymentMethod = paymentMethod.name,
    frequency = frequency.name,
    nextDueDateEpochMillis = nextDueDateEpochMillis,
    isActive = isActive,
    autoAdd = autoAdd
)
