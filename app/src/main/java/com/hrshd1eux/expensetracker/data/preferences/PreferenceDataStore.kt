package com.hrshd1eux.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class LockType {
    PIN,
    PATTERN,
    PASSWORD
}

interface UserPreferences {
    val currencyCodeFlow: Flow<String>
    val currencySymbolFlow: Flow<String>
    suspend fun setCurrency(code: String, symbol: String)
    suspend fun setLastPaymentMethod(method: String)
    suspend fun addRecentCategoryId(categoryId: String)
}

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferenceDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferences {
    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        internal val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        internal val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_LAST_PAYMENT_METHOD = stringPreferencesKey("last_payment_method")
        private val KEY_RECENT_CATEGORY_IDS = stringPreferencesKey("recent_category_ids")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_LOCK_TYPE = stringPreferencesKey("lock_type")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PATTERN_SALT = stringPreferencesKey("pattern_salt")
        private val KEY_PATTERN_HASH = stringPreferencesKey("pattern_hash")
        private val KEY_PASSWORD_SALT = stringPreferencesKey("password_salt")
        private val KEY_PASSWORD_HASH = stringPreferencesKey("password_hash")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOCK_TIMEOUT_MINUTES = intPreferencesKey("lock_timeout_minutes")
        private val KEY_SCREEN_SECURITY = booleanPreferencesKey("screen_security")
        private val KEY_FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
        private val KEY_FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        private val KEY_MONTHLY_BUDGET = androidx.datastore.preferences.core.longPreferencesKey("monthly_budget_paise")
        private val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        private val KEY_DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            AppTheme.LIGHT.name -> AppTheme.LIGHT
            AppTheme.DARK.name -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    override val currencyCodeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_CODE] ?: "INR"
    }

    override val currencySymbolFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_SYMBOL] ?: "₹"
    }

    val lastPaymentMethodFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_PAYMENT_METHOD] ?: "UPI"
    }

    val recentCategoryIdsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_CATEGORY_IDS] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split(",")
    }

    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val lockTimeoutMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCK_TIMEOUT_MINUTES] ?: 0
    }

    val screenSecurityFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCREEN_SECURITY] ?: false
    }

    val firstLaunchCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH_COMPLETED] ?: false
    }

    val pinSaltFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_SALT]
    }

    val pinHashFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_HASH]
    }

    val lockTypeFlow: Flow<LockType> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_LOCK_TYPE]) {
            LockType.PATTERN.name -> LockType.PATTERN
            LockType.PASSWORD.name -> LockType.PASSWORD
            else -> LockType.PIN
        }
    }

    val patternSaltFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_PATTERN_SALT] }
    val patternHashFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_PATTERN_HASH] }
    val passwordSaltFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_PASSWORD_SALT] }
    val passwordHashFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_PASSWORD_HASH] }

    val monthlyBudgetFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_MONTHLY_BUDGET] ?: 0L
    }

    val dailyReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_REMINDER_ENABLED] ?: false
    }

    val dailyReminderTimeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_REMINDER_TIME] ?: "21:30"
    }

    val firstDayOfWeekFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_DAY_OF_WEEK] ?: 1 // 1 = Monday
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setCurrency(code: String, symbol: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY_CODE] = code
            prefs[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    override suspend fun setLastPaymentMethod(method: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_PAYMENT_METHOD] = method
        }
    }

    override suspend fun addRecentCategoryId(categoryId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_CATEGORY_IDS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(categoryId) + current.filter { it != categoryId }).take(6)
            prefs[KEY_RECENT_CATEGORY_IDS] = updated.joinToString(",")
        }
    }

    suspend fun setAppLock(
        enabled: Boolean,
        salt: String? = null,
        hash: String? = null,
        biometricEnabled: Boolean = false,
        timeoutMinutes: Int = 0
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            if (salt != null) prefs[KEY_PIN_SALT] = salt
            if (hash != null) prefs[KEY_PIN_HASH] = hash
            prefs[KEY_BIOMETRIC_ENABLED] = biometricEnabled
            prefs[KEY_LOCK_TIMEOUT_MINUTES] = timeoutMinutes
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setLockTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCK_TIMEOUT_MINUTES] = minutes
        }
    }

    suspend fun setScreenSecurity(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCREEN_SECURITY] = enabled
        }
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH_COMPLETED] = completed
        }
    }

    suspend fun setFirstDayOfWeek(dayOfWeek: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_DAY_OF_WEEK] = dayOfWeek
        }
    }

    suspend fun setLockType(type: LockType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOCK_TYPE] = type.name
        }
    }

    suspend fun setPatternLock(salt: String, hash: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = true
            prefs[KEY_LOCK_TYPE] = LockType.PATTERN.name
            prefs[KEY_PATTERN_SALT] = salt
            prefs[KEY_PATTERN_HASH] = hash
        }
    }

    suspend fun setPasswordLock(salt: String, hash: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = true
            prefs[KEY_LOCK_TYPE] = LockType.PASSWORD.name
            prefs[KEY_PASSWORD_SALT] = salt
            prefs[KEY_PASSWORD_HASH] = hash
        }
    }

    suspend fun setMonthlyBudget(budgetPaise: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONTHLY_BUDGET] = budgetPaise
        }
    }

    suspend fun setDailyReminder(enabled: Boolean, time: String = "21:30") {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_REMINDER_ENABLED] = enabled
            prefs[KEY_DAILY_REMINDER_TIME] = time
        }
    }
}
