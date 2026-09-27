package com.hrshd1eux.expensetracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.usecase.AddCategoryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.ArchiveCategoryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetCategoriesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val isArchivedVisible: Boolean = false,
    val isLoading: Boolean = true
)

sealed class CategoryEvent {
    data class ShowMessage(val text: String) : CategoryEvent()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val archiveCategoryUseCase: ArchiveCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CategoryEvent>()
    val eventFlow: SharedFlow<CategoryEvent> = _eventFlow.asSharedFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase(includeArchived = true).collect { list ->
                _uiState.update { it.copy(categories = list, isLoading = false) }
            }
        }
    }

    fun toggleShowArchived() {
        _uiState.update { it.copy(isArchivedVisible = !it.isArchivedVisible) }
    }

    fun addCategory(name: String, icon: String, color: Long) {
        viewModelScope.launch {
            val result = addCategoryUseCase(name, icon, color)
            result.fold(
                onSuccess = { _eventFlow.emit(CategoryEvent.ShowMessage("Category '${it.name}' created")) },
                onFailure = { _eventFlow.emit(CategoryEvent.ShowMessage(it.localizedMessage ?: "Failed")) }
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            updateCategoryUseCase(category)
            _eventFlow.emit(CategoryEvent.ShowMessage("Category updated"))
        }
    }

    fun setArchived(id: String, archive: Boolean) {
        viewModelScope.launch {
            archiveCategoryUseCase(id, archive)
            _eventFlow.emit(CategoryEvent.ShowMessage(if (archive) "Category archived" else "Category restored"))
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val deleted = deleteCategoryUseCase(id)
            if (deleted) {
                _eventFlow.emit(CategoryEvent.ShowMessage("Category deleted"))
            } else {
                _eventFlow.emit(CategoryEvent.ShowMessage("Category has expenses: archived instead of deleted"))
            }
        }
    }
}
