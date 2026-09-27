package com.hrshd1eux.expensetracker.presentation.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.domain.usecase.ExportBackupUseCase
import com.hrshd1eux.expensetracker.domain.usecase.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject

import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import com.hrshd1eux.expensetracker.util.MoneyUtils
import kotlinx.coroutines.flow.first

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val pendingImportUri: Uri? = null,
    val pendingImportContent: String? = null,
    val isPasswordPromptOpen: Boolean = false,
    val isExportPasswordDialogOpen: Boolean = false
)

sealed class BackupEvent {
    data class ShowSuccess(val message: String) : BackupEvent()
    data class ShowError(val message: String) : BackupEvent()
    object TriggerExportFilePicker : BackupEvent()
    object TriggerCsvExportFilePicker : BackupEvent()
    object TriggerImportFilePicker : BackupEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BackupEvent>()
    val eventFlow: SharedFlow<BackupEvent> = _eventFlow.asSharedFlow()

    private var pendingExportPassword: String? = null

    fun onExportClicked() {
        _uiState.update { it.copy(isExportPasswordDialogOpen = true) }
    }

    fun dismissExportPasswordDialog() {
        _uiState.update { it.copy(isExportPasswordDialogOpen = false) }
    }

    fun proceedExportWithPassword(password: String?) {
        pendingExportPassword = if (password.isNullOrBlank()) null else password
        _uiState.update { it.copy(isExportPasswordDialogOpen = false) }
        viewModelScope.launch {
            _eventFlow.emit(BackupEvent.TriggerExportFilePicker)
        }
    }

    fun onImportClicked() {
        viewModelScope.launch {
            _eventFlow.emit(BackupEvent.TriggerImportFilePicker)
        }
    }

    fun writeBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = exportBackupUseCase(pendingExportPassword)
            result.fold(
                onSuccess = { jsonContent ->
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                OutputStreamWriter(stream).use { writer ->
                                    writer.write(jsonContent)
                                }
                            }
                        }
                        _eventFlow.emit(BackupEvent.ShowSuccess("Backup exported successfully!"))
                    } catch (e: Exception) {
                        _eventFlow.emit(BackupEvent.ShowError("Failed to write backup file: ${e.localizedMessage}"))
                    }
                },
                onFailure = { err ->
                    _eventFlow.emit(BackupEvent.ShowError(err.localizedMessage ?: "Failed to generate backup"))
                }
            )
            _uiState.update { it.copy(isExporting = false) }
            pendingExportPassword = null
        }
    }

    fun readBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            reader.readText()
                        }
                    } ?: ""
                }

                executeImport(content, null)
            } catch (e: Exception) {
                _eventFlow.emit(BackupEvent.ShowError("Could not read backup file: ${e.localizedMessage}"))
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun executeImport(content: String, password: String?) {
        viewModelScope.launch {
            when (val res = importBackupUseCase(content, password)) {
                is ImportBackupUseCase.ImportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isPasswordPromptOpen = false,
                            pendingImportContent = null
                        )
                    }
                    _eventFlow.emit(
                        BackupEvent.ShowSuccess(
                            "Restored ${res.expenseCount} expenses and ${res.categoryCount} categories!"
                        )
                    )
                }
                is ImportBackupUseCase.ImportResult.PasswordRequired -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isPasswordPromptOpen = true,
                            pendingImportContent = content
                        )
                    }
                }
                is ImportBackupUseCase.ImportResult.Error -> {
                    _uiState.update { it.copy(isImporting = false) }
                    _eventFlow.emit(BackupEvent.ShowError(res.message))
                }
            }
        }
    }

    fun dismissPasswordPrompt() {
        _uiState.update { it.copy(isPasswordPromptOpen = false, pendingImportContent = null) }
    }

    fun onExportCsvClicked() {
        viewModelScope.launch {
            _eventFlow.emit(BackupEvent.TriggerCsvExportFilePicker)
        }
    }

    fun writeCsvToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val expenses = expenseRepository.getAllExpensesSync()
                val categories = categoryRepository.getAllCategories().first().associateBy { it.id }

                val sb = StringBuilder()
                sb.append("Date,Time,Category,Amount,Payment Method,Note,Reimbursable\n")

                for (exp in expenses) {
                    val catName = categories[exp.categoryId]?.name ?: "Expense"
                    val dateStr = DateTimeUtils.formatDate(exp.timestamp)
                    val timeStr = DateTimeUtils.formatTime(exp.timestamp)
                    val amountStr = MoneyUtils.formatPaiseToEditable(exp.amountPaise)
                    val methodStr = exp.paymentMethod.displayName
                    val noteEscaped = exp.note.replace("\"", "\"\"")
                    val reimbursableStr = if (exp.isReimbursable) "Yes" else "No"

                    sb.append("\"$dateStr\",\"$timeStr\",\"$catName\",$amountStr,\"$methodStr\",\"$noteEscaped\",\"$reimbursableStr\"\n")
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream).use { writer ->
                            writer.write(sb.toString())
                        }
                    }
                }
                _eventFlow.emit(BackupEvent.ShowSuccess("CSV exported successfully!"))
            } catch (e: Exception) {
                _eventFlow.emit(BackupEvent.ShowError("Failed to export CSV: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }
}
