package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.data.preferences.UserPreferences
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import java.util.UUID
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        if (expense.amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (expense.categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Category must be selected"))
        }

        val finalizedExpense = if (expense.id.isBlank()) {
            expense.copy(id = UUID.randomUUID().toString())
        } else {
            expense
        }

        expenseRepository.addExpense(finalizedExpense)

        // Remember smart defaults
        userPreferences.setLastPaymentMethod(finalizedExpense.paymentMethod.name)
        userPreferences.addRecentCategoryId(finalizedExpense.categoryId)

        return Result.success(Unit)
    }
}
