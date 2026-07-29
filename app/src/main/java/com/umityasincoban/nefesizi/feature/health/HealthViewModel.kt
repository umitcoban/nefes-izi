package com.umityasincoban.nefesizi.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.domain.HealthAssociation
import com.umityasincoban.nefesizi.core.domain.HealthMeasurementDraft
import com.umityasincoban.nefesizi.core.domain.calculateHealthJournalSummary
import com.umityasincoban.nefesizi.core.domain.validateHealthMeasurements
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthUiState(
    val selectedDate: String = "",
    val energy: Int? = null,
    val stress: Int? = null,
    val sleep: Int? = null,
    val morningCough: Boolean? = null,
    val headache: Boolean? = null,
    val shortnessOfBreath: Boolean? = null,
    val chestDiscomfort: Boolean? = null,
    val restingHeartRate: String = "",
    val exerciseMinutes: String = "",
    val systolicBloodPressure: String = "",
    val diastolicBloodPressure: String = "",
    val weightKg: String = "",
    val note: String = "",
    val recordedDays7: Int = 0,
    val recordedDays14: Int = 0,
    val recordedDays30: Int = 0,
    val associations: List<HealthAssociation> = emptyList(),
    val noteHistory: List<DailyHealthEntryEntity> = emptyList(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val warningConfirmationRequired: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val clock: Clock,
) : ViewModel() {
    private val zone = clock.zone
    private val today = LocalDate.now(clock)
    private val selectedDate = MutableStateFlow(today)
    private var original: DailyHealthEntryEntity? = null
    private var formTouched = false
    private val _state = MutableStateFlow(HealthUiState(selectedDate = today.toString()))
    val state: StateFlow<HealthUiState> = _state.asStateFlow()
    private val savedChannel = Channel<Boolean>(Channel.BUFFERED)
    val saved = savedChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            selectedDate.flatMapLatest { repository.observeHealthEntry(it.toString()) }
                .collect(::loadEntry)
        }
        viewModelScope.launch {
            combine(
                repository.observeHealthEntries(today.minusDays(29).toString(), today.toString()),
                repository.observeAllRecords(),
            ) { entries, records ->
                entries to calculateHealthJournalSummary(entries, records, today)
            }.collect { (entries, summary) ->
                _state.update {
                    it.copy(
                        recordedDays7 = summary.recordedDays7,
                        recordedDays14 = summary.recordedDays14,
                        recordedDays30 = summary.recordedDays30,
                        associations = summary.associations,
                        noteHistory = entries.filter { entry -> !entry.note.isNullOrBlank() }
                            .sortedByDescending(DailyHealthEntryEntity::entryDate),
                    )
                }
            }
        }
    }

    fun selectDate(value: String) {
        val date = runCatching { LocalDate.parse(value.trim()) }.getOrNull() ?: run {
            _state.update { it.copy(errors = listOf("Tarihi YYYY-AA-GG biçiminde gir.")) }
            return
        }
        if (date > today) {
            _state.update { it.copy(errors = listOf("Gelecek güne sağlık kaydı eklenemez.")) }
            return
        }
        formTouched = false
        original = null
        _state.value = HealthUiState(
            selectedDate = date.toString(),
            recordedDays7 = _state.value.recordedDays7,
            recordedDays14 = _state.value.recordedDays14,
            recordedDays30 = _state.value.recordedDays30,
            associations = _state.value.associations,
            noteHistory = _state.value.noteHistory,
        )
        selectedDate.value = date
    }

    fun updateDateText(value: String) {
        _state.update { it.copy(selectedDate = value.take(10), errors = emptyList()) }
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1).toString())
    fun nextDay() {
        if (selectedDate.value < today) selectDate(selectedDate.value.plusDays(1).toString())
    }

    fun setEnergy(value: Int) = edit { copy(energy = value) }
    fun setStress(value: Int) = edit { copy(stress = value) }
    fun setSleep(value: Int) = edit { copy(sleep = value) }
    fun setMorningCough(value: Boolean?) = edit { copy(morningCough = value) }
    fun setHeadache(value: Boolean?) = edit { copy(headache = value) }
    fun setShortnessOfBreath(value: Boolean?) = edit { copy(shortnessOfBreath = value) }
    fun setChestDiscomfort(value: Boolean?) = edit { copy(chestDiscomfort = value) }
    fun setHeartRate(value: String) = edit { copy(restingHeartRate = digits(value, 3)) }
    fun setExercise(value: String) = edit { copy(exerciseMinutes = digits(value, 3)) }
    fun setSystolic(value: String) = edit { copy(systolicBloodPressure = digits(value, 3)) }
    fun setDiastolic(value: String) = edit { copy(diastolicBloodPressure = digits(value, 3)) }
    fun setWeight(value: String) = edit {
        copy(weightKg = value.filter { it.isDigit() || it == ',' || it == '.' }.take(7))
    }
    fun setNote(value: String) = edit { copy(note = value.take(500)) }

    fun save(confirmWarnings: Boolean = false) {
        val value = _state.value
        val draft = value.measurementDraft()
        val validation = validateHealthMeasurements(draft)
        if (!validation.canSave) {
            _state.update { it.copy(errors = validation.errors, warnings = validation.warnings) }
            return
        }
        if (validation.warnings.isNotEmpty() && !confirmWarnings) {
            _state.update {
                it.copy(
                    errors = emptyList(),
                    warnings = validation.warnings,
                    warningConfirmationRequired = true,
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSaving = true,
                    errors = emptyList(),
                    warnings = emptyList(),
                    warningConfirmationRequired = false,
                )
            }
            val now = clock.millis()
            val result = runCatching {
                repository.saveHealthEntry(
                    DailyHealthEntryEntity(
                        entryDate = selectedDate.value.toString(),
                        zoneId = zone.id,
                        energyLevel = value.energy,
                        stressLevel = value.stress,
                        sleepQuality = value.sleep,
                        morningCough = value.morningCough,
                        headache = value.headache,
                        shortnessOfBreath = value.shortnessOfBreath,
                        chestDiscomfort = value.chestDiscomfort,
                        restingHeartRate = draft.restingHeartRate,
                        exerciseMinutes = draft.exerciseMinutes,
                        systolicBloodPressure = draft.systolicBloodPressure,
                        diastolicBloodPressure = draft.diastolicBloodPressure,
                        weightGrams = draft.weightGrams,
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

    private fun loadEntry(entry: DailyHealthEntryEntity?) {
        original = entry
        if (formTouched) return
        _state.update {
            it.copy(
                energy = entry?.energyLevel,
                stress = entry?.stressLevel,
                sleep = entry?.sleepQuality,
                morningCough = entry?.morningCough,
                headache = entry?.headache,
                shortnessOfBreath = entry?.shortnessOfBreath,
                chestDiscomfort = entry?.chestDiscomfort,
                restingHeartRate = entry?.restingHeartRate?.toString().orEmpty(),
                exerciseMinutes = entry?.exerciseMinutes?.toString().orEmpty(),
                systolicBloodPressure = entry?.systolicBloodPressure?.toString().orEmpty(),
                diastolicBloodPressure = entry?.diastolicBloodPressure?.toString().orEmpty(),
                weightKg = entry?.weightGrams?.let { grams ->
                    BigDecimal.valueOf(grams, 3).stripTrailingZeros().toPlainString()
                }.orEmpty(),
                note = entry?.note.orEmpty(),
                errors = emptyList(),
                warnings = emptyList(),
                warningConfirmationRequired = false,
            )
        }
    }

    private fun HealthUiState.measurementDraft() = HealthMeasurementDraft(
        restingHeartRate = restingHeartRate.toIntOrNull(),
        exerciseMinutes = exerciseMinutes.toIntOrNull(),
        systolicBloodPressure = systolicBloodPressure.toIntOrNull(),
        diastolicBloodPressure = diastolicBloodPressure.toIntOrNull(),
        weightGrams = weightKg.replace(',', '.').toBigDecimalOrNull()
            ?.movePointRight(3)
            ?.longValueExactOrNull(),
    )

    private fun edit(block: HealthUiState.() -> HealthUiState) {
        formTouched = true
        _state.update { it.block().copy(errors = emptyList(), warningConfirmationRequired = false) }
    }
}

private fun digits(value: String, max: Int) = value.filter(Char::isDigit).take(max)

private fun BigDecimal.longValueExactOrNull(): Long? =
    runCatching { longValueExact() }.getOrNull()
