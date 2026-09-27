package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class TodaySummary(
    val totalPaise: Long = 0L,
    val upiTotalPaise: Long = 0L,
    val cashTotalPaise: Long = 0L,
    val expenses: List<ExpenseWithCategory> = emptyList()
)

class GetTodaySummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(): Flow<TodaySummary> {
        return combine(
            expenseRepository.getTodayTotalPaise(),
            expenseRepository.getTodayUpiTotalPaise(),
            expenseRepository.getTodayCashTotalPaise(),
            expenseRepository.getTodayExpenses()
        ) { total, upi, cash, expenses ->
            TodaySummary(
                totalPaise = total,
                upiTotalPaise = upi,
                cashTotalPaise = cash,
                expenses = expenses
            )
        }
    }
}
