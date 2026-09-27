package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import javax.inject.Inject

class GetExpenseByIdUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(id: String): ExpenseWithCategory? {
        return expenseRepository.getExpenseById(id)
    }
}
