package com.umityasincoban.nefesizi.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.domain.calculateExposure
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DailyCount(val date: LocalDate, val count: Int)

data class AnalyticsUiState(
    val total: Int = 0,
    val dailyAverage: Double = 0.0,
    val longestGapMinutes: Long? = null,
    val peakHour: Int? = null,
    val nicotineMicrograms: Long? = null,
    val nicotineKnown: Int = 0,
    val nicotineUnknown: Int = 0,
    val lastSevenDays: List<DailyCount> = emptyList(),
    val hasEnoughData: Boolean = false,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: NefesIziRepository,
    clock: Clock,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(clock)
    private val startDate = today.minusDays(29)
    private val startInstant = startDate.atStartOfDay(zone).toInstant()

    val state: StateFlow<AnalyticsUiState> = repository.observeAllRecords().map { allRecords ->
        val records = allRecords.filter { it.smokedAtEpochMillis >= startInstant.toEpochMilli() }
        val byDate = records.groupBy {
            java.time.Instant.ofEpochMilli(it.smokedAtEpochMillis)
                .atZone(ZoneId.of(it.zoneIdSnapshot))
                .toLocalDate()
        }
        val daily = (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            DailyCount(date, byDate[date].orEmpty().sumOf { it.quantity })
        }
        val total = records.sumOf { it.quantity }
        val sorted = records.sortedBy { it.smokedAtEpochMillis }
        val longest = sorted.zipWithNext { first, second ->
            Duration.ofMillis(second.smokedAtEpochMillis - first.smokedAtEpochMillis).toMinutes()
        }.maxOrNull()
        val peakHour = records
            .groupingBy {
                java.time.Instant.ofEpochMilli(it.smokedAtEpochMillis)
                    .atZone(ZoneId.of(it.zoneIdSnapshot)).hour
            }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        val nicotine = calculateExposure(records) { it.nicotineMicrogramsPerCigaretteSnapshot }
        AnalyticsUiState(
            total = total,
            dailyAverage = total / 30.0,
            longestGapMinutes = longest,
            peakHour = peakHour,
            nicotineMicrograms = nicotine.micrograms,
            nicotineKnown = nicotine.knownCount,
            nicotineUnknown = nicotine.unknownCount,
            lastSevenDays = daily,
            hasEnoughData = byDate.size >= 7,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())
}
