package com.hrshd1eux.expensetracker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.hrshd1eux.expensetracker.data.preferences.AppTheme
import com.hrshd1eux.expensetracker.data.preferences.LockType
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.presentation.main.MainScreen
import com.hrshd1eux.expensetracker.presentation.security.LockScreen
import com.hrshd1eux.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.hrshd1eux.expensetracker.security.AppLockManager
import com.hrshd1eux.expensetracker.security.KeystoreManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferenceDataStore: PreferenceDataStore

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var keystoreManager: KeystoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by preferenceDataStore.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val dynamicColor by preferenceDataStore.dynamicColorFlow.collectAsStateWithLifecycle(initialValue = true)
            val screenSecurity by preferenceDataStore.screenSecurityFlow.collectAsStateWithLifecycle(initialValue = false)
            val isAppLockEnabled by preferenceDataStore.appLockEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
            val isBiometricEnabled by preferenceDataStore.biometricEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
            val lockType by preferenceDataStore.lockTypeFlow.collectAsStateWithLifecycle(initialValue = LockType.PIN)
            val pinSalt by preferenceDataStore.pinSaltFlow.collectAsStateWithLifecycle(initialValue = null)
            val pinHash by preferenceDataStore.pinHashFlow.collectAsStateWithLifecycle(initialValue = null)
            val patternSalt by preferenceDataStore.patternSaltFlow.collectAsStateWithLifecycle(initialValue = null)
            val patternHash by preferenceDataStore.patternHashFlow.collectAsStateWithLifecycle(initialValue = null)
            val passwordSalt by preferenceDataStore.passwordSaltFlow.collectAsStateWithLifecycle(initialValue = null)
            val passwordHash by preferenceDataStore.passwordHashFlow.collectAsStateWithLifecycle(initialValue = null)
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

            // Dynamic Window FLAG_SECURE for screenshot / recent app preview protection
            LaunchedEffect(screenSecurity) {
                if (screenSecurity) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val darkTheme = when (themeMode) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ExpenseTrackerTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                if (isAppLockEnabled && isLocked) {
                    LockScreen(
                        lockType = lockType,
                        onUnlock = { appLockManager.unlock() },
                        onVerifyCredential = { credential ->
                            val (salt, hash) = when (lockType) {
                                LockType.PIN -> Pair(pinSalt, pinHash)
                                LockType.PATTERN -> Pair(patternSalt, patternHash)
                                LockType.PASSWORD -> Pair(passwordSalt, passwordHash)
                            }
                            if (salt != null && hash != null) {
                                keystoreManager.verifyPin(credential, salt, hash)
                            } else {
                                false
                            }
                        },
                        isBiometricEnabled = isBiometricEnabled
                    )
                } else {
                    MainScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val enabled = preferenceDataStore.appLockEnabledFlow.first()
            val timeout = preferenceDataStore.lockTimeoutMinutesFlow.first()
            appLockManager.onAppEnteredForeground(enabled, timeout)
        }
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onAppEnteredBackground()
    }
}
