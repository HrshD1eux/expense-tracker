package com.hrshd1eux.expensetracker.presentation.addexpense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.usecase.AddExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.DeleteExpenseUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetCategoriesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetExpenseByIdUseCase
import com.hrshd1eux.expensetracker.domain.usecase.UpdateExpenseUseCase
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import com.hrshd1eux.expensetracker.util.MoneyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository

data class AddExpenseUiState(
    val isEditMode: Boolean = false,
    val expenseId: String? = null,
    val amountInput: String = "",
    val calculatedPreview: String? = null,
    val duplicateWarning: String? = null,
    val isReimbursable: Boolean = false,
    val selectedCategoryId: String = "",
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.UPI,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val categories: List<Category> = emptyList(),
    val recentCategories: List<Category> = emptyList(),
    val currencySymbol: String = "₹",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed class AddExpenseEvent {
    object SavedSuccessfully : AddExpenseEvent()
    object DeletedSuccessfully : AddExpenseEvent()
    data class ShowError(val message: String) : AddExpenseEvent()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val expenseRepository: ExpenseRepository,
    private val preferenceDataStore: PreferenceDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val expenseIdArg: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(AddExpenseUiState(expenseId = expenseIdArg))
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddExpenseEvent>()
    val eventFlow: SharedFlow<AddExpenseEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val symbol = preferenceDataStore.currencySymbolFlow.first()
            val lastPayment = preferenceDataStore.lastPaymentMethodFlow.first()
            val recentIds = preferenceDataStore.recentCategoryIdsFlow.first()

            val defaultPayment = try {
                PaymentMethod.valueOf(lastPayment)
            } catch (e: Exception) {
                PaymentMethod.UPI
            }

            kotlinx.coroutines.flow.combine(
                getCategoriesUseCase(includeArchived = false),
                expenseRepository.getCategoryUsageCounts()
            ) { allCats, usageMap ->
                val sortedCats = allCats.sortedByDescending { usageMap[it.id] ?: 0 }
                val recentCats = recentIds.mapNotNull { id -> sortedCats.find { it.id == id } }
                val initialCatId = if (sortedCats.isNotEmpty()) sortedCats.first().id else ""

                _uiState.update { current ->
                    current.copy(
                        categories = sortedCats,
                        recentCategories = recentCats,
                        currencySymbol = symbol,
                        selectedPaymentMethod = if (!current.isEditMode) defaultPayment else current.selectedPaymentMethod,
                        selectedCategoryId = if (current.selectedCategoryId.isEmpty()) initialCatId else current.selectedCategoryId
                    )
                }

                // If editing existing expense
                if (!expenseIdArg.isNullOrBlank()) {
                    val existing = getExpenseByIdUseCase(expenseIdArg)
                    if (existing != null) {
                        val activeAndExisting = if (sortedCats.none { it.id == existing.expense.categoryId } && existing.category != null) {
                            sortedCats + existing.category
                        } else {
                            sortedCats
                        }
                        _uiState.update { current ->
                            current.copy(
                                categories = activeAndExisting,
                                isEditMode = true,
                                amountInput = MoneyUtils.formatPaiseToEditable(existing.expense.amountPaise),
                                selectedCategoryId = existing.expense.categoryId,
                                selectedPaymentMethod = existing.expense.paymentMethod,
                                note = existing.expense.note,
                                timestamp = existing.expense.timestamp
                            )
                        }
                    }
                }
            }.collect {}
        }
    }

    fun onAmountChanged(input: String) {
        // Normalize commas to decimal point, asterisks/slashes to × and ÷
        val normalized = input
            .replace(',', '.')
            .replace('*', '×')
            .replace('/', '÷')

        // Allow digits, decimals, spaces, and math operators
        val allowedChars = "0123456789.+-×÷ "
        val filtered = normalized.filter { it in allowedChars }
        val hasOperators = MoneyUtils.hasMathOperators(filtered)
        val preview = if (hasOperators) {
            MoneyUtils.evaluateExpressionToString(filtered)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                amountInput = filtered,
                calculatedPreview = preview,
                errorMessage = null
            )
        }
        checkDuplicate(filtered, _uiState.value.selectedCategoryId, _uiState.value.selectedPaymentMethod)
    }

    fun appendMathOperator(operator: String) {
        val current = _uiState.value.amountInput.trim()
        if (current.isEmpty()) return
        val lastChar = current.last()
        val updated = if (lastChar in "+-×÷") {
            current.dropLast(1) + " $operator "
        } else {
            "$current $operator "
        }
        onAmountChanged(updated)
    }

    fun appendDecimal() {
        val current = _uiState.value.amountInput
        if (current.isEmpty()) {
            onAmountChanged("0.")
            return
        }
        val lastToken = current.split(' ', '+', '-', '×', '÷').lastOrNull() ?: ""
        if (!lastToken.contains('.')) {
            onAmountChanged("$current.")
        }
    }

    fun onClearAmount() {
        _uiState.update {
            it.copy(
                amountInput = "",
                calculatedPreview = null,
                duplicateWarning = null,
                errorMessage = null
            )
        }
    }

    fun onEvaluateMath() {
        val current = _uiState.value.amountInput
        val evaluated = MoneyUtils.evaluateExpressionToString(current)
        if (evaluated != null) {
            _uiState.update {
                it.copy(
                    amountInput = evaluated,
                    calculatedPreview = null,
                    errorMessage = null
                )
            }
            checkDuplicate(evaluated, _uiState.value.selectedCategoryId, _uiState.value.selectedPaymentMethod)
        }
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
        checkDuplicate(_uiState.value.amountInput, categoryId, _uiState.value.selectedPaymentMethod)
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
        checkDuplicate(_uiState.value.amountInput, _uiState.value.selectedCategoryId, method)
    }

    fun onReimbursableChanged(isReimbursable: Boolean) {
        _uiState.update { it.copy(isReimbursable = isReimbursable) }
    }

    fun onNoteChanged(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
    }

    fun onTimestampChanged(newTimestamp: Long) {
        _uiState.update { it.copy(timestamp = newTimestamp) }
    }

    private fun checkDuplicate(amountStr: String, catId: String, method: PaymentMethod) {
        if (_uiState.value.isEditMode || catId.isBlank()) {
            _uiState.update { it.copy(duplicateWarning = null) }
            return
        }
        val paise = MoneyUtils.evaluateExpressionToPaise(amountStr) ?: MoneyUtils.parseAmountToPaise(amountStr)
        if (paise <= 0L) {
            _uiState.update { it.copy(duplicateWarning = null) }
            return
        }

        viewModelScope.launch {
            val tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000L)
            val duplicate = expenseRepository.findRecentSimilarExpense(
                categoryId = catId,
                amountPaise = paise,
                paymentMethod = method,
                sinceEpochMillis = tenMinutesAgo
            )
            _uiState.update {
                it.copy(
                    duplicateWarning = if (duplicate != null) {
                        "Note: An identical expense was recorded a few minutes ago."
                    } else null
                )
            }
        }
    }

    fun saveExpense() {
        val state = _uiState.value
        val amountPaise = MoneyUtils.evaluateExpressionToPaise(state.amountInput)
            ?: MoneyUtils.parseAmountToPaise(state.amountInput)

        if (amountPaise <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please enter an amount greater than zero") }
            return
        }

        if (state.selectedCategoryId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a category") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val expense = Expense(
                id = state.expenseId ?: "",
                amountPaise = amountPaise,
                categoryId = state.selectedCategoryId,
                paymentMethod = state.selectedPaymentMethod,
                note = state.note.trim(),
                timestamp = state.timestamp,
                isReimbursable = state.isReimbursable
            )

            val result = if (state.isEditMode) {
                updateExpenseUseCase(expense)
            } else {
                addExpenseUseCase(expense)
            }

            result.fold(
                onSuccess = {
                    _eventFlow.emit(AddExpenseEvent.SavedSuccessfully)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = err.localizedMessage) }
                    _eventFlow.emit(AddExpenseEvent.ShowError(err.localizedMessage ?: "Failed to save"))
                }
            )
        }
    }

    fun deleteExpense() {
        val id = _uiState.value.expenseId ?: return
        viewModelScope.launch {
            deleteExpenseUseCase(id)
            _eventFlow.emit(AddExpenseEvent.DeletedSuccessfully)
        }
    }
}
