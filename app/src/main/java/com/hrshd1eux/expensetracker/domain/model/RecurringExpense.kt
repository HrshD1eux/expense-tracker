package com.hrshd1eux.expensetracker.domain.model

enum class RecurringFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class RecurringExpense(
    val id: Long = 0L,
    val title: String,
    val amountPaise: Long,
    val categoryId: String,
    val paymentMethod: PaymentMethod,
    val frequency: RecurringFrequency,
    val nextDueDateEpochMillis: Long,
    val isActive: Boolean = true,
    val autoAdd: Boolean = true
)
