package com.umityasincoban.nefesizi.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.AppPreferences
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import com.umityasincoban.nefesizi.core.domain.ExposureTotal
import com.umityasincoban.nefesizi.core.domain.TodaySummary
import com.umityasincoban.nefesizi.core.domain.calculateExposure
import com.umityasincoban.nefesizi.core.domain.calculateTodaySummary
import com.umityasincoban.nefesizi.core.domain.logicalDayRange
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    val selectedQuickProduct: CigaretteProductEntity? = null,
    val products: List<CigaretteProductEntity> = emptyList(),
    val records: List<SmokingRecordEntity> = emptyList(),
    val summary: TodaySummary = calculateTodaySummary(emptyList()),
    val nicotine: ExposureTotal = ExposureTotal(null, 0, 0),
    val tar: ExposureTotal = ExposureTotal(null, 0, 0),
    val carbonMonoxide: ExposureTotal = ExposureTotal(null, 0, 0),
    val showCost: Boolean = true,
    val showExposure: Boolean = true,
    val isLogging: Boolean = false,
    val dayStartHour: Int = 0,
) {
    val totalCount: Int get() = summary.totalCount
}

sealed interface TodayEffect {
    data class RecordCreated(val id: String) : TodayEffect
    data object ProductRequired : TodayEffect
    data object SaveFailed : TodayEffect
}

private data class TodayProducts(
    val default: CigaretteProductEntity?,
    val all: List<CigaretteProductEntity>,
    val selected: CigaretteProductEntity?,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    preferences: AppPreferences,
    private val clock: Clock,
) : ViewModel() {
    private val logMutex = Mutex()
    private val isLogging = MutableStateFlow(false)
    private val selectedProductId = MutableStateFlow<String?>(null)
    private val effectsChannel = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = effectsChannel.receiveAsFlow()

    private val zone = clock.zone
    private val products = combine(
        repository.observeDefaultProduct(),
        repository.observeProducts(),
        selectedProductId,
    ) { default, all, selectedId ->
        TodayProducts(
            default = default,
            all = all,
            selected = all.firstOrNull { it.id == selectedId } ?: default ?: all.firstOrNull(),
        )
    }

    val state: StateFlow<TodayUiState> = combine(
        products,
        repository.observeAllRecords(),
        preferences.todayDisplayPreferences,
        isLogging,
        preferences.personalization,
    ) { productState, allRecords, display, logging, personalization ->
        val range = logicalDayRange(clock.instant(), zone, personalization.dayStartHour)
        val records = allRecords.filter {
            val instant = java.time.Instant.ofEpochMilli(it.smokedAtEpochMillis)
            !instant.isBefore(range.start) && instant.isBefore(range.endExclusive)
        }
        TodayUiState(
            isLoading = false,
            defaultProduct = productState.default,
            selectedQuickProduct = productState.selected,
            products = productState.all,
            records = records,
            summary = calculateTodaySummary(records),
            nicotine = calculateExposure(records) {
                it.nicotineMicrogramsPerCigaretteSnapshot
            },
            tar = calculateExposure(records) {
                it.tarMicrogramsPerCigaretteSnapshot
            },
            carbonMonoxide = calculateExposure(records) {
                it.carbonMonoxideMicrogramsPerCigaretteSnapshot
            },
            showCost = display.showCost,
            showExposure = display.showExposure,
            isLogging = logging,
            dayStartHour = personalization.dayStartHour,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun selectQuickProduct(id: String) {
        selectedProductId.value = id
    }

    fun logCigarette() {
        viewModelScope.launch {
            if (!logMutex.tryLock()) return@launch
            isLogging.value = true
            try {
                val product = state.value.selectedQuickProduct
                val record = if (product == null) {
                    null
                } else {
                    repository.logWithProduct(product.id)
                }
                effectsChannel.send(
                    if (record == null) {
                        TodayEffect.ProductRequired
                    } else {
                        TodayEffect.RecordCreated(record.id)
                    },
                )
            } catch (_: Exception) {
                effectsChannel.send(TodayEffect.SaveFailed)
            } finally {
                isLogging.value = false
                logMutex.unlock()
            }
        }
    }

    fun undo(id: String) {
        viewModelScope.launch { logMutex.withLock { repository.undoRecord(id) } }
    }
}
