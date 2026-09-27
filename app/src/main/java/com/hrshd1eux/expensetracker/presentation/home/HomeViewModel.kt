package com.hrshd1eux.expensetracker.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.usecase.DeleteExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetCategoriesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetTodaySummaryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.RestoreExpenseUseCase
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import java.time.LocalDate

data class HomeUiState(
    val greeting: String = "",
    val currentDate: String = "",
    val todayTotalPaise: Long = 0L,
    val upiTotalPaise: Long = 0L,
    val cashTotalPaise: Long = 0L,
    val expenses: List<ExpenseWithCategory> = emptyList(),
    val monthlyBudgetPaise: Long = 0L,
    val monthTotalPaise: Long = 0L,
    val dailySafeToSpendPaise: Long = 0L,
    val remainingBudgetPaise: Long = 0L,
    val budgetProgress: Float = 0f,
    val daysLeftInMonth: Int = 1,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val isFirstLaunch: Boolean = false,
    val isLoading: Boolean = true
)

sealed class HomeUiEvent {
    data class ShowUndoSnackbar(val message: String, val expense: Expense) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodaySummaryUseCase: GetTodaySummaryUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val restoreExpenseUseCase: RestoreExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val expenseRepository: ExpenseRepository,
    private val preferenceDataStore: PreferenceDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            greeting = DateTimeUtils.getGreeting(),
            currentDate = DateTimeUtils.formatCurrentHeaderDate()
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeUiEvent>()
    val eventFlow: SharedFlow<HomeUiEvent> = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            getCategoriesUseCase.ensureDefaultCategories()
        }

        val monthStart = DateTimeUtils.getMonthStartEpochMillis()
        val monthEnd = DateTimeUtils.getMonthEndEpochMillis()

        combine(
            getTodaySummaryUseCase(),
            expenseRepository.getTotalForDateRange(monthStart, monthEnd),
            preferenceDataStore.monthlyBudgetFlow,
            preferenceDataStore.currencySymbolFlow,
            preferenceDataStore.currencyCodeFlow,
            preferenceDataStore.firstLaunchCompletedFlow
        ) { args: Array<Any?> ->
            val summary = args[0] as com.hrshd1eux.expensetracker.domain.usecase.TodaySummary
            val monthTotal = args[1] as Long
            val budgetPaise = args[2] as Long
            val symbol = args[3] as String
            val code = args[4] as String
            val firstLaunchDone = args[5] as Boolean

            val now = LocalDate.now()
            val daysInMonth = now.lengthOfMonth()
            val dayOfMonth = now.dayOfMonth
            val daysLeft = (daysInMonth - dayOfMonth + 1).coerceAtLeast(1)

            val remaining = budgetPaise - monthTotal
            val safeToSpend = if (remaining > 0L) remaining / daysLeft else 0L
            val progress = if (budgetPaise > 0L) {
                (monthTotal.toFloat() / budgetPaise.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            _uiState.update { current ->
                current.copy(
                    greeting = DateTimeUtils.getGreeting(),
                    currentDate = DateTimeUtils.formatCurrentHeaderDate(),
                    todayTotalPaise = summary.totalPaise,
                    upiTotalPaise = summary.upiTotalPaise,
                    cashTotalPaise = summary.cashTotalPaise,
                    expenses = summary.expenses,
                    monthlyBudgetPaise = budgetPaise,
                    monthTotalPaise = monthTotal,
                    dailySafeToSpendPaise = safeToSpend,
                    remainingBudgetPaise = remaining,
                    budgetProgress = progress,
                    daysLeftInMonth = daysLeft,
                    currencySymbol = symbol,
                    currencyCode = code,
                    isFirstLaunch = !firstLaunchDone,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun updateMonthlyBudget(budgetPaise: Long) {
        viewModelScope.launch {
            preferenceDataStore.setMonthlyBudget(budgetPaise)
        }
    }

    fun deleteExpense(item: ExpenseWithCategory) {
        viewModelScope.launch {
            deleteExpenseUseCase(item.expense.id)
            _eventFlow.emit(
                HomeUiEvent.ShowUndoSnackbar(
                    message = "Expense deleted",
                    expense = item.expense
                )
            )
        }
    }

    fun undoDelete(expense: Expense) {
        viewModelScope.launch {
            restoreExpenseUseCase(expense)
        }
    }

    fun dismissFirstLaunch() {
        viewModelScope.launch {
            preferenceDataStore.setFirstLaunchCompleted(true)
        }
    }
}
