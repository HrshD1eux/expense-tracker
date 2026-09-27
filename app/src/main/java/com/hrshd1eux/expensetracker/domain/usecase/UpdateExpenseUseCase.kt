package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        if (expense.amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (expense.categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Category must be selected"))
        }

        val updated = expense.copy(updatedAt = System.currentTimeMillis())
        expenseRepository.updateExpense(updated)
        return Result.success(Unit)
    }
}
