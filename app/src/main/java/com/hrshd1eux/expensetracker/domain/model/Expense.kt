package com.hrshd1eux.expensetracker.domain.model

data class Expense(
    val id: String,
    val amountPaise: Long,
    val categoryId: String,
    val paymentMethod: PaymentMethod,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isReimbursable: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ExpenseWithCategory(
    val expense: Expense,
    val category: Category?
)
