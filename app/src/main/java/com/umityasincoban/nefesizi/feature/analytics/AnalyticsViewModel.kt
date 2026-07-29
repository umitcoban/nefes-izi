package com.umityasincoban.nefesizi.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.domain.AdvancedAnalytics
import com.umityasincoban.nefesizi.core.domain.CurrencyAmount
import com.umityasincoban.nefesizi.core.domain.calculateAdvancedAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import com.umityasincoban.nefesizi.core.data.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AnalyticsPeriod(val label: String, val days: Long?) {
    DAYS_7("7 gün", 7),
    DAYS_30("30 gün", 30),
    DAYS_90("90 gün", 90),
    ALL("Tümü", null),
    CUSTOM("Özel", null),
}

data class AnalyticsSelection(
    val period: AnalyticsPeriod = AnalyticsPeriod.DAYS_30,
    val customStart: String = "",
    val customEnd: String = "",
)

data class AnalyticsUiState(
    val selection: AnalyticsSelection = AnalyticsSelection(),
    val analytics: AdvancedAnalytics? = null,
    val todayCosts: List<CurrencyAmount> = emptyList(),
    val weekCosts: List<CurrencyAmount> = emptyList(),
    val monthCosts: List<CurrencyAmount> = emptyList(),
    val dateError: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: NefesIziRepository,
    preferences: AppPreferences,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val selection = MutableStateFlow(AnalyticsSelection())

    val state: StateFlow<AnalyticsUiState> = combine(
        repository.observeAllRecords(),
        selection,
        preferences.wakeTime,
    ) { records, selected, wakeTimeValue ->
        val requestedRange = selected.resolveRange(today)
        val range = requestedRange ?: AnalyticsPeriod.DAYS_30.resolveRange(today)
        val wakeTime = wakeTimeValue?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val analytics = calculateAdvancedAnalytics(records, range.first, range.second, wakeTime)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val monthStart = today.withDayOfMonth(1)
        AnalyticsUiState(
            selection = selected,
            analytics = analytics,
            todayCosts = calculateAdvancedAnalytics(records, today, today).costs,
            weekCosts = calculateAdvancedAnalytics(records, weekStart, today).costs,
            monthCosts = calculateAdvancedAnalytics(records, monthStart, today).costs,
            dateError = if (selected.period == AnalyticsPeriod.CUSTOM && requestedRange == null) {
                "Başlangıç ve bitiş tarihini YYYY-AA-GG biçiminde gir."
            } else {
                null
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun selectPeriod(period: AnalyticsPeriod) {
        selection.value = selection.value.copy(period = period)
    }

    fun updateCustomStart(value: String) {
        selection.value = selection.value.copy(
            period = AnalyticsPeriod.CUSTOM,
            customStart = value.take(10),
        )
    }

    fun updateCustomEnd(value: String) {
        selection.value = selection.value.copy(
            period = AnalyticsPeriod.CUSTOM,
            customEnd = value.take(10),
        )
    }
}

private fun AnalyticsSelection.resolveRange(today: LocalDate): Pair<LocalDate?, LocalDate>? =
    when (period) {
        AnalyticsPeriod.CUSTOM -> {
            val start = customStart.toDateOrNull()
            val end = customEnd.toDateOrNull()
            if (start == null || end == null || start > end || end > today) null else start to end
        }
        else -> period.resolveRange(today)
    }

private fun AnalyticsPeriod.resolveRange(today: LocalDate): Pair<LocalDate?, LocalDate> =
    days?.let { today.minusDays(it - 1) } to today

private fun String.toDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()
