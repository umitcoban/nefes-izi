package com.umityasincoban.nefesizi.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import com.umityasincoban.nefesizi.core.domain.ExposureTotal
import com.umityasincoban.nefesizi.core.domain.calculateExposure
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TodayUiState(
    val isLoading: Boolean = true,
    val defaultProduct: CigaretteProductEntity? = null,
    val records: List<SmokingRecordEntity> = emptyList(),
    val totalCount: Int = 0,
    val nicotine: ExposureTotal = ExposureTotal(null, 0, 0),
    val tar: ExposureTotal = ExposureTotal(null, 0, 0),
    val carbonMonoxide: ExposureTotal = ExposureTotal(null, 0, 0),
    val isLogging: Boolean = false,
)

sealed interface TodayEffect {
    data class RecordCreated(val id: String) : TodayEffect
    data object ProductRequired : TodayEffect
    data object SaveFailed : TodayEffect
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val clock: Clock,
) : ViewModel() {
    private val logMutex = Mutex()
    private val effectsChannel = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = effectsChannel.receiveAsFlow()

    private val zone = ZoneId.systemDefault()
    private val today = clock.instant().atZone(zone).toLocalDate()
    private val start = today.atStartOfDay(zone).toInstant()
    private val end = today.plusDays(1).atStartOfDay(zone).toInstant()

    val state: StateFlow<TodayUiState> = combine(
        repository.observeDefaultProduct(),
        repository.observeRecords(start, end),
    ) { product, records ->
        TodayUiState(
            isLoading = false,
            defaultProduct = product,
            records = records,
            totalCount = records.sumOf { it.quantity },
            nicotine = calculateExposure(records) { it.nicotineMicrogramsPerCigaretteSnapshot },
            tar = calculateExposure(records) { it.tarMicrogramsPerCigaretteSnapshot },
            carbonMonoxide = calculateExposure(records) { it.carbonMonoxideMicrogramsPerCigaretteSnapshot },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun logCigarette() {
        viewModelScope.launch {
            if (!logMutex.tryLock()) return@launch
            try {
                val record = repository.logWithDefaultProduct()
                effectsChannel.send(
                    if (record == null) TodayEffect.ProductRequired else TodayEffect.RecordCreated(record.id),
                )
            } catch (_: Exception) {
                effectsChannel.send(TodayEffect.SaveFailed)
            } finally {
                logMutex.unlock()
            }
        }
    }

    fun undo(id: String) {
        viewModelScope.launch { logMutex.withLock { repository.undoRecord(id) } }
    }
}
