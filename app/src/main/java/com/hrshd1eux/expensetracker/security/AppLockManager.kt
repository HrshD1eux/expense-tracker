package com.hrshd1eux.expensetracker.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class LockTimeout(val minutes: Int, val label: String) {
    IMMEDIATELY(0, "Immediately"),
    ONE_MINUTE(1, "After 1 minute"),
    FIVE_MINUTES(5, "After 5 minutes")
}

@Singleton
class AppLockManager @Inject constructor() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimeMillis: Long = 0L

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }

    fun onAppEnteredBackground() {
        lastBackgroundTimeMillis = System.currentTimeMillis()
    }

    fun onAppEnteredForeground(appLockEnabled: Boolean, timeoutMinutes: Int) {
        if (!appLockEnabled) {
            _isLocked.value = false
            return
        }

        if (lastBackgroundTimeMillis == 0L) {
            // First open with lock enabled
            _isLocked.value = true
            return
        }

        val elapsedMillis = System.currentTimeMillis() - lastBackgroundTimeMillis
        val timeoutMillis = timeoutMinutes * 60 * 1000L

        if (elapsedMillis >= timeoutMillis) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastBackgroundTimeMillis = 0L
    }
}
