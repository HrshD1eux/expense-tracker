package com.hrshd1eux.expensetracker.presentation.security

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.security.AppLockManager
import com.hrshd1eux.expensetracker.security.BiometricPromptHelper
import com.hrshd1eux.expensetracker.security.KeystoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.hrshd1eux.expensetracker.data.preferences.LockType

data class SecurityUiState(
    val isAppLockEnabled: Boolean = false,
    val lockType: LockType = LockType.PIN,
    val hasPinSet: Boolean = false,
    val hasPatternSet: Boolean = false,
    val hasPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val timeoutMinutes: Int = 0,
    val isPinSetupDialogOpen: Boolean = false,
    val isPatternSetupDialogOpen: Boolean = false,
    val isPasswordSetupDialogOpen: Boolean = false
)

sealed class SecurityEvent {
    data class ShowMessage(val message: String) : SecurityEvent()
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val preferenceDataStore: PreferenceDataStore,
    private val keystoreManager: KeystoreManager,
    private val appLockManager: AppLockManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SecurityEvent>()
    val eventFlow: SharedFlow<SecurityEvent> = _eventFlow.asSharedFlow()

    init {
        val biometricAvailable = BiometricPromptHelper.isBiometricAvailable(context)

        viewModelScope.launch {
            combine(
                preferenceDataStore.appLockEnabledFlow,
                preferenceDataStore.lockTypeFlow,
                preferenceDataStore.pinHashFlow,
                preferenceDataStore.patternHashFlow,
                preferenceDataStore.passwordHashFlow,
                preferenceDataStore.biometricEnabledFlow,
                preferenceDataStore.lockTimeoutMinutesFlow
            ) { args: Array<Any?> ->
                val lockEnabled = args[0] as Boolean
                val type = args[1] as LockType
                val pinHash = args[2] as? String
                val patternHash = args[3] as? String
                val passHash = args[4] as? String
                val bioEnabled = args[5] as Boolean
                val timeout = args[6] as Int

                _uiState.update { current ->
                    current.copy(
                        isAppLockEnabled = lockEnabled,
                        lockType = type,
                        hasPinSet = !pinHash.isNullOrBlank(),
                        hasPatternSet = !patternHash.isNullOrBlank(),
                        hasPasswordSet = !passHash.isNullOrBlank(),
                        isBiometricEnabled = bioEnabled,
                        isBiometricAvailable = biometricAvailable,
                        timeoutMinutes = timeout
                    )
                }
            }.collect {}
        }
    }

    fun selectLockType(type: LockType) {
        viewModelScope.launch {
            preferenceDataStore.setLockType(type)
            when (type) {
                LockType.PIN -> if (!_uiState.value.hasPinSet) openPinSetup()
                LockType.PATTERN -> if (!_uiState.value.hasPatternSet) openPatternSetup()
                LockType.PASSWORD -> if (!_uiState.value.hasPasswordSet) openPasswordSetup()
            }
        }
    }

    fun openPinSetup() {
        _uiState.update { it.copy(isPinSetupDialogOpen = true) }
    }

    fun closePinSetup() {
        _uiState.update { it.copy(isPinSetupDialogOpen = false) }
    }

    fun openPatternSetup() {
        _uiState.update { it.copy(isPatternSetupDialogOpen = true) }
    }

    fun closePatternSetup() {
        _uiState.update { it.copy(isPatternSetupDialogOpen = false) }
    }

    fun openPasswordSetup() {
        _uiState.update { it.copy(isPasswordSetupDialogOpen = true) }
    }

    fun closePasswordSetup() {
        _uiState.update { it.copy(isPasswordSetupDialogOpen = false) }
    }

    fun saveNewPin(pin: String) {
        if (pin.length < 4) {
            viewModelScope.launch {
                _eventFlow.emit(SecurityEvent.ShowMessage("PIN must be at least 4 digits"))
            }
            return
        }

        viewModelScope.launch {
            val salt = keystoreManager.generateSalt()
            val hash = keystoreManager.hashPin(pin, salt)

            preferenceDataStore.setAppLock(
                enabled = true,
                salt = salt,
                hash = hash,
                biometricEnabled = _uiState.value.isBiometricEnabled,
                timeoutMinutes = _uiState.value.timeoutMinutes
            )
            preferenceDataStore.setLockType(LockType.PIN)

            _uiState.update { it.copy(isPinSetupDialogOpen = false) }
            _eventFlow.emit(SecurityEvent.ShowMessage("PIN saved & App Lock enabled"))
        }
    }

    fun saveNewPattern(pattern: String) {
        viewModelScope.launch {
            val salt = keystoreManager.generateSalt()
            val hash = keystoreManager.hashPin(pattern, salt)
            preferenceDataStore.setPatternLock(salt, hash)
            _uiState.update { it.copy(isPatternSetupDialogOpen = false) }
            _eventFlow.emit(SecurityEvent.ShowMessage("Pattern lock saved"))
        }
    }

    fun saveNewPassword(password: String) {
        if (password.length < 4) {
            viewModelScope.launch {
                _eventFlow.emit(SecurityEvent.ShowMessage("Password must be at least 4 characters"))
            }
            return
        }
        viewModelScope.launch {
            val salt = keystoreManager.generateSalt()
            val hash = keystoreManager.hashPin(password, salt)
            preferenceDataStore.setPasswordLock(salt, hash)
            _uiState.update { it.copy(isPasswordSetupDialogOpen = false) }
            _eventFlow.emit(SecurityEvent.ShowMessage("Password lock saved"))
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !_uiState.value.hasPinSet) {
                // Must set a PIN first before enabling lock
                _uiState.update { it.copy(isPinSetupDialogOpen = true) }
            } else {
                preferenceDataStore.setAppLock(enabled = enabled)
                if (!enabled) {
                    appLockManager.unlock()
                }
                _eventFlow.emit(
                    SecurityEvent.ShowMessage(
                        if (enabled) "App lock enabled" else "App lock disabled"
                    )
                )
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferenceDataStore.setBiometricEnabled(enabled)
        }
    }

    fun setTimeoutMinutes(minutes: Int) {
        viewModelScope.launch {
            preferenceDataStore.setLockTimeoutMinutes(minutes)
        }
    }
}
