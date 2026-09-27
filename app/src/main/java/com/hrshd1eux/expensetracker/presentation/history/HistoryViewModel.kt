package com.hrshd1eux.expensetracker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.usecase.DateGroupedExpenses
import com.hrshd1eux.expensetracker.domain.usecase.DeleteExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetCategoriesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetHistoryExpensesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.HistoryFilter
import com.hrshd1eux.expensetracker.domain.usecase.RestoreExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter(),
    val groupedExpenses: List<DateGroupedExpenses> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = true
)

sealed class HistoryUiEvent {
    data class ShowUndoSnackbar(val message: String, val expense: Expense) : HistoryUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryExpensesUseCase: GetHistoryExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val restoreExpenseUseCase: RestoreExpenseUseCase,
    private val preferenceDataStore: PreferenceDataStore
) : ViewModel() {

    private val _filterState = MutableStateFlow(HistoryFilter())
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HistoryUiEvent>()
    val eventFlow: SharedFlow<HistoryUiEvent> = _eventFlow.asSharedFlow()

    init {
        // Load categories and preferences
        viewModelScope.launch {
            combine(
                getCategoriesUseCase(includeArchived = true),
                preferenceDataStore.currencySymbolFlow,
                preferenceDataStore.currencyCodeFlow
            ) { categories, symbol, code ->
                _uiState.update { current ->
                    current.copy(
                        allCategories = categories,
                        currencySymbol = symbol,
                        currencyCode = code
                    )
                }
            }.collect {}
        }

        // Observe filter and reload expenses
        viewModelScope.launch {
            _filterState.flatMapLatest { filter ->
                getHistoryExpensesUseCase(filter)
            }.collect { grouped ->
                _uiState.update { current ->
                    current.copy(
                        groupedExpenses = grouped,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
        _uiState.update { it.copy(filter = _filterState.value) }
    }

    fun setFilterSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isFilterSheetVisible = visible) }
    }

    fun applyFilters(
        paymentMethod: PaymentMethod?,
        categoryIds: Set<String>,
        startMillis: Long?,
        endMillis: Long?,
        minAmountPaise: Long?,
        maxAmountPaise: Long?
    ) {
        _filterState.update {
            it.copy(
                paymentMethod = paymentMethod,
                selectedCategoryIds = categoryIds,
                startMillis = startMillis,
                endMillis = endMillis,
                minAmountPaise = minAmountPaise,
                maxAmountPaise = maxAmountPaise
            )
        }
        _uiState.update {
            it.copy(
                filter = _filterState.value,
                isFilterSheetVisible = false
            )
        }
    }

    fun resetFilters() {
        val currentQuery = _filterState.value.searchQuery
        val cleared = HistoryFilter(searchQuery = currentQuery)
        _filterState.value = cleared
        _uiState.update { it.copy(filter = cleared, isFilterSheetVisible = false) }
    }

    fun deleteExpense(item: ExpenseWithCategory) {
        viewModelScope.launch {
            deleteExpenseUseCase(item.expense.id)
            _eventFlow.emit(
                HistoryUiEvent.ShowUndoSnackbar(
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
}
