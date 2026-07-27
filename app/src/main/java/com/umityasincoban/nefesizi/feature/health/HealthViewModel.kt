package com.umityasincoban.nefesizi.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthUiState(
    val energy: Int? = null,
    val stress: Int? = null,
    val sleep: Int? = null,
    val morningCough: Boolean? = null,
    val headache: Boolean? = null,
    val shortnessOfBreath: Boolean? = null,
    val chestDiscomfort: Boolean? = null,
    val restingHeartRate: String = "",
    val exerciseMinutes: String = "",
    val note: String = "",
    val recordedDaysLastWeek: Int = 0,
    val isSaving: Boolean = false,
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val clock: Clock,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(clock)
    private var original: DailyHealthEntryEntity? = null
    private var formTouched = false
    private val _state = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()
    private val savedChannel = Channel<Boolean>(Channel.BUFFERED)
    val saved = savedChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeHealthEntry(today.toString()).collect { entry ->
                original = entry
                if (!formTouched && entry != null) {
                    _state.update {
                        it.copy(
                            energy = entry.energyLevel,
                            stress = entry.stressLevel,
                            sleep = entry.sleepQuality,
                            morningCough = entry.morningCough,
                            headache = entry.headache,
                            shortnessOfBreath = entry.shortnessOfBreath,
                            chestDiscomfort = entry.chestDiscomfort,
                            restingHeartRate = entry.restingHeartRate?.toString().orEmpty(),
                            exerciseMinutes = entry.exerciseMinutes?.toString().orEmpty(),
                            note = entry.note.orEmpty(),
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.observeHealthEntries(today.minusDays(6).toString(), today.toString())
                .collect { entries -> _state.update { it.copy(recordedDaysLastWeek = entries.size) } }
        }
    }

    fun setEnergy(value: Int) = edit { copy(energy = value) }
    fun setStress(value: Int) = edit { copy(stress = value) }
    fun setSleep(value: Int) = edit { copy(sleep = value) }
    fun setMorningCough(value: Boolean?) = edit { copy(morningCough = value) }
    fun setHeadache(value: Boolean?) = edit { copy(headache = value) }
    fun setShortnessOfBreath(value: Boolean?) = edit { copy(shortnessOfBreath = value) }
    fun setChestDiscomfort(value: Boolean?) = edit { copy(chestDiscomfort = value) }
    fun setHeartRate(value: String) = edit { copy(restingHeartRate = value.filter(Char::isDigit).take(3)) }
    fun setExercise(value: String) = edit { copy(exerciseMinutes = value.filter(Char::isDigit).take(3)) }
    fun setNote(value: String) = edit { copy(note = value.take(500)) }

    fun save() {
        val value = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val now = clock.millis()
            val result = runCatching {
                repository.saveHealthEntry(
                    DailyHealthEntryEntity(
                        entryDate = today.toString(),
                        zoneId = zone.id,
                        energyLevel = value.energy,
                        stressLevel = value.stress,
                        sleepQuality = value.sleep,
                        morningCough = value.morningCough,
                        headache = value.headache,
                        shortnessOfBreath = value.shortnessOfBreath,
                        chestDiscomfort = value.chestDiscomfort,
                        restingHeartRate = value.restingHeartRate.toIntOrNull(),
                        exerciseMinutes = value.exerciseMinutes.toIntOrNull(),
                        note = value.note.trim().takeIf(String::isNotBlank),
                        createdAtEpochMillis = original?.createdAtEpochMillis ?: now,
                        updatedAtEpochMillis = now,
                    ),
                )
            }.isSuccess
            _state.update { it.copy(isSaving = false) }
            savedChannel.send(result)
        }
    }

    private fun edit(block: HealthUiState.() -> HealthUiState) {
        formTouched = true
        _state.update(block)
    }
}
