package com.hrshd1eux.expensetracker.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore
import com.hrshd1eux.expensetracker.domain.usecase.AnalyticsData
import com.hrshd1eux.expensetracker.domain.usecase.AnalyticsPeriod
import com.hrshd1eux.expensetracker.domain.usecase.GetAnalyticsUseCase
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.THIS_MONTH,
    val analyticsData: AnalyticsData? = null,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
    // Custom range — epoch millis for start-of-day and end-of-day
    val customStartMillis: Long = DateTimeUtils.getMonthStartEpochMillis(),
    val customEndMillis: Long = DateTimeUtils.getDayEndEpochMillis()
)

/** Bundles period + custom range so flatMapLatest re-triggers on any change. */
private data class PeriodQuery(
    val period: AnalyticsPeriod,
    val customStart: Long,
    val customEnd: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val preferenceDataStore: PreferenceDataStore
) : ViewModel() {

    private val _query = MutableStateFlow(
        PeriodQuery(
            period = AnalyticsPeriod.THIS_MONTH,
            customStart = DateTimeUtils.getMonthStartEpochMillis(),
            customEnd = DateTimeUtils.getDayEndEpochMillis()
        )
    )
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferenceDataStore.currencySymbolFlow,
                preferenceDataStore.currencyCodeFlow
            ) { symbol, code ->
                _uiState.update { it.copy(currencySymbol = symbol, currencyCode = code) }
            }.collect {}
        }

        viewModelScope.launch {
            _query.flatMapLatest { q ->
                getAnalyticsUseCase(
                    period = q.period,
                    customStartMillis = q.customStart,
                    customEndMillis = q.customEnd
                )
            }.collect { data ->
                _uiState.update { current ->
                    current.copy(
                        selectedPeriod = data.period,
                        analyticsData = data,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onPeriodSelected(period: AnalyticsPeriod) {
        _query.update { it.copy(period = period) }
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
    }

    fun onCustomRangeSelected(startMillis: Long, endMillis: Long) {
        _query.update {
            it.copy(
                period = AnalyticsPeriod.CUSTOM,
                customStart = startMillis,
                customEnd = endMillis
            )
        }
        _uiState.update {
            it.copy(
                selectedPeriod = AnalyticsPeriod.CUSTOM,
                customStartMillis = startMillis,
                customEndMillis = endMillis,
                isLoading = true
            )
        }
    }
}
