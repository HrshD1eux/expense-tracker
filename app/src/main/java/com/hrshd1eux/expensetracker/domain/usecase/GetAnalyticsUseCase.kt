package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import com.hrshd1eux.expensetracker.util.MoneyUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

enum class AnalyticsPeriod(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom")
}

data class CategorySpend(
    val category: Category?,
    val amountPaise: Long,
    val percentage: Float
)

data class DailySpend(
    val date: LocalDate,
    val totalPaise: Long,
    val label: String
)

data class HourlySpend(
    val hour: Int,
    val totalPaise: Long,
    val label: String
)

data class AnalyticsData(
    val period: AnalyticsPeriod,
    val startMillis: Long,
    val endMillis: Long,
    val totalSpendingPaise: Long = 0L,
    val averageDailySpendingPaise: Long = 0L,
    val highestSpendingDay: Pair<LocalDate, Long>? = null,
    val largestExpense: ExpenseWithCategory? = null,
    val categorySpends: List<CategorySpend> = emptyList(),
    val upiSpendingPaise: Long = 0L,
    val upiPercentage: Float = 0f,
    val cashSpendingPaise: Long = 0L,
    val cashPercentage: Float = 0f,
    val dailyBreakdown: List<DailySpend> = emptyList(),
    val hourlyBreakdown: List<HourlySpend> = emptyList()
)

class GetAnalyticsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(
        period: AnalyticsPeriod,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null
    ): Flow<AnalyticsData> {
        val (startMillis, endMillis) = when (period) {
            AnalyticsPeriod.TODAY -> Pair(
                DateTimeUtils.getDayStartEpochMillis(),
                DateTimeUtils.getDayEndEpochMillis()
            )
            AnalyticsPeriod.THIS_WEEK -> Pair(
                DateTimeUtils.getWeekStartEpochMillis(),
                DateTimeUtils.getWeekEndEpochMillis()
            )
            AnalyticsPeriod.THIS_MONTH -> Pair(
                DateTimeUtils.getMonthStartEpochMillis(),
                DateTimeUtils.getMonthEndEpochMillis()
            )
            AnalyticsPeriod.CUSTOM -> Pair(
                customStartMillis ?: DateTimeUtils.getMonthStartEpochMillis(),
                customEndMillis ?: DateTimeUtils.getDayEndEpochMillis()
            )
        }

        return expenseRepository.getExpensesForDateRange(startMillis, endMillis).map { expenses ->
            calculateAnalytics(period, startMillis, endMillis, expenses)
        }
    }

    internal fun calculateAnalytics(
        period: AnalyticsPeriod,
        startMillis: Long,
        endMillis: Long,
        expenses: List<ExpenseWithCategory>
    ): AnalyticsData {
        if (expenses.isEmpty()) {
            return AnalyticsData(
                period = period,
                startMillis = startMillis,
                endMillis = endMillis
            )
        }

        val totalSpendingPaise = expenses.sumOf { it.expense.amountPaise }

        // 1. Sanitize and clamp date ranges to prevent inverted or invalid loops
        val actualStartMillis = minOf(startMillis, endMillis)
        val actualEndMillis = maxOf(startMillis, endMillis)

        val startDate = DateTimeUtils.getLocalDate(actualStartMillis)
        val endDate = DateTimeUtils.getLocalDate(actualEndMillis)
        val today = LocalDate.now()
        val effectiveEndDate = if (endDate.isAfter(today)) today else endDate
        val effectiveStartDate = if (startDate.isAfter(effectiveEndDate)) effectiveEndDate else startDate
        val daysBetween = (ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1).toInt().coerceAtLeast(1)
        val averageDailySpendingPaise = MoneyUtils.calculateDailyAverage(totalSpendingPaise, daysBetween)

        // Largest expense
        val largestExpense = expenses.maxByOrNull { it.expense.amountPaise }

        // Highest spending day & daily breakdown
        val dayExpensesMap = expenses.groupBy {
            DateTimeUtils.getLocalDate(it.expense.timestamp)
        }

        val highestDayEntry = dayExpensesMap.maxByOrNull { entry ->
            entry.value.sumOf { it.expense.amountPaise }
        }
        val highestSpendingDay = highestDayEntry?.let { entry ->
            Pair(entry.key, entry.value.sumOf { it.expense.amountPaise })
        }

        // Daily breakdown
        val shortFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

        val dailyList = mutableListOf<DailySpend>()
        var curDate = effectiveStartDate
        while (!curDate.isAfter(effectiveEndDate)) {
            val dayTotal = dayExpensesMap[curDate]?.sumOf { it.expense.amountPaise } ?: 0L
            val label = if (daysBetween <= 7) curDate.format(dayFormatter) else curDate.format(shortFormatter)
            dailyList.add(DailySpend(date = curDate, totalPaise = dayTotal, label = label))
            curDate = curDate.plusDays(1)
        }

        // Hourly breakdown (for 0 to 23 hours)
        val hourMap = expenses.groupBy { DateTimeUtils.getHourOfDay(it.expense.timestamp) }
        val hourlyList = (0..23).map { hour ->
            val totalInHour = hourMap[hour]?.sumOf { it.expense.amountPaise } ?: 0L
            val label = when (hour) {
                0 -> "12 AM"
                in 1..11 -> "$hour AM"
                12 -> "12 PM"
                else -> "${hour - 12} PM"
            }
            HourlySpend(hour = hour, totalPaise = totalInHour, label = label)
        }

        // Spending by category
        val categorySpends = expenses.groupBy { it.expense.categoryId }
            .map { (catId, catExpenses) ->
                val amount = catExpenses.sumOf { it.expense.amountPaise }
                val percentage = if (totalSpendingPaise > 0L) (amount.toFloat() / totalSpendingPaise) * 100f else 0f
                val cat = catExpenses.firstOrNull()?.category
                CategorySpend(category = cat, amountPaise = amount, percentage = percentage)
            }
            .sortedByDescending { it.amountPaise }

        // Spending by payment method
        val upiPaise = expenses
            .filter { it.expense.paymentMethod == PaymentMethod.UPI }
            .sumOf { it.expense.amountPaise }
        val cashPaise = expenses
            .filter { it.expense.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.expense.amountPaise }

        val upiPercentage = if (totalSpendingPaise > 0L) (upiPaise.toFloat() / totalSpendingPaise) * 100f else 0f
        val cashPercentage = if (totalSpendingPaise > 0L) (cashPaise.toFloat() / totalSpendingPaise) * 100f else 0f

        return AnalyticsData(
            period = period,
            startMillis = startMillis,
            endMillis = endMillis,
            totalSpendingPaise = totalSpendingPaise,
            averageDailySpendingPaise = averageDailySpendingPaise,
            highestSpendingDay = highestSpendingDay,
            largestExpense = largestExpense,
            categorySpends = categorySpends,
            upiSpendingPaise = upiPaise,
            upiPercentage = upiPercentage,
            cashSpendingPaise = cashPaise,
            cashPercentage = cashPercentage,
            dailyBreakdown = dailyList,
            hourlyBreakdown = hourlyList
        )
    }
}
