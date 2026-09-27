package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class HistoryFilter(
    val searchQuery: String = "",
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val paymentMethod: PaymentMethod? = null,
    val minAmountPaise: Long? = null,
    val maxAmountPaise: Long? = null
)

data class DateGroupedExpenses(
    val dateHeader: String,
    val dateEpochMillis: Long,
    val totalPaise: Long,
    val expenses: List<ExpenseWithCategory>
)

class GetHistoryExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(filter: HistoryFilter = HistoryFilter()): Flow<List<DateGroupedExpenses>> {
        val baseFlow = if (filter.startMillis != null && filter.endMillis != null) {
            expenseRepository.getExpensesForDateRange(filter.startMillis, filter.endMillis)
        } else {
            expenseRepository.getAllExpenses()
        }

        return baseFlow.map { list ->
            val query = filter.searchQuery.trim().lowercase()

            val filtered = list.filter { item ->
                // Search query match (note, category name, payment method, amount)
                val matchesQuery = if (query.isEmpty()) true else {
                    item.expense.note.lowercase().contains(query) ||
                            item.category?.name?.lowercase()?.contains(query) == true ||
                            item.expense.paymentMethod.displayName.lowercase().contains(query) ||
                            (item.expense.amountPaise / 100L).toString().contains(query)
                }

                // Category filter
                val matchesCategory = if (filter.selectedCategoryIds.isEmpty()) true else {
                    filter.selectedCategoryIds.contains(item.expense.categoryId)
                }

                // Payment method filter
                val matchesPayment = if (filter.paymentMethod == null) true else {
                    item.expense.paymentMethod == filter.paymentMethod
                }

                // Amount filter
                val matchesMin = if (filter.minAmountPaise == null) true else {
                    item.expense.amountPaise >= filter.minAmountPaise
                }
                val matchesMax = if (filter.maxAmountPaise == null) true else {
                    item.expense.amountPaise <= filter.maxAmountPaise
                }

                matchesQuery && matchesCategory && matchesPayment && matchesMin && matchesMax
            }

            // Group by formatted date header (chronological order)
            val groups = filtered.groupBy { item ->
                DateTimeUtils.getDayStartEpochMillis(DateTimeUtils.getLocalDate(item.expense.timestamp))
            }

            groups.entries
                .sortedByDescending { it.key }
                .map { (dayStartMillis, dayExpenses) ->
                    val totalPaise = dayExpenses.sumOf { it.expense.amountPaise }
                    val header = DateTimeUtils.getHistoryDateHeader(dayStartMillis)
                    DateGroupedExpenses(
                        dateHeader = header,
                        dateEpochMillis = dayStartMillis,
                        totalPaise = totalPaise,
                        expenses = dayExpenses
                    )
                }
        }
    }
}
