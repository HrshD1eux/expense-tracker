package com.hrshd1eux.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.data.preferences.AppTheme
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
    val appLockEnabled: Boolean = false,
    val screenSecurityEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceDataStore: PreferenceDataStore,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val appearanceFlow = combine(
        preferenceDataStore.themeFlow,
        preferenceDataStore.dynamicColorFlow,
        preferenceDataStore.currencyCodeFlow,
        preferenceDataStore.currencySymbolFlow
    ) { theme, dynamicColor, code, symbol ->
        SettingsUiState(
            theme = theme,
            dynamicColor = dynamicColor,
            currencyCode = code,
            currencySymbol = symbol
        )
    }

    private val securityFlow = combine(
        preferenceDataStore.appLockEnabledFlow,
        preferenceDataStore.screenSecurityFlow,
        preferenceDataStore.lockTimeoutMinutesFlow
    ) { lockEnabled, screenSec, timeout ->
        Triple(lockEnabled, screenSec, timeout)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceFlow,
        securityFlow
    ) { base, (lockEnabled, screenSec, timeout) ->
        base.copy(
            appLockEnabled = lockEnabled,
            screenSecurityEnabled = screenSec,
            lockTimeoutMinutes = timeout
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferenceDataStore.setTheme(theme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferenceDataStore.setDynamicColor(enabled)
        }
    }

    fun setCurrency(code: String, symbol: String) {
        viewModelScope.launch {
            preferenceDataStore.setCurrency(code, symbol)
            try {
                com.hrshd1eux.expensetracker.widget.ExpenseWidget().updateAll(context)
            } catch (e: Exception) {
                // Ignore widget failure
            }
        }
    }

    fun setScreenSecurity(enabled: Boolean) {
        viewModelScope.launch {
            preferenceDataStore.setScreenSecurity(enabled)
        }
    }

    private val _updateStatus = kotlinx.coroutines.flow.MutableStateFlow<com.hrshd1eux.expensetracker.util.UpdateCheckResult>(
        com.hrshd1eux.expensetracker.util.UpdateCheckResult.Idle
    )
    val updateStatus: StateFlow<com.hrshd1eux.expensetracker.util.UpdateCheckResult> = _updateStatus

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateStatus.value = com.hrshd1eux.expensetracker.util.UpdateCheckResult.Checking
            val result = com.hrshd1eux.expensetracker.util.UpdateChecker.checkForUpdates()
            _updateStatus.value = result
        }
    }

    fun clearUpdateStatus() {
        _updateStatus.value = com.hrshd1eux.expensetracker.util.UpdateCheckResult.Idle
    }
}
