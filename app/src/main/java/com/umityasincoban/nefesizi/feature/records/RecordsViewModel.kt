package com.umityasincoban.nefesizi.feature.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import com.umityasincoban.nefesizi.core.domain.SmokingRecordDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecordPeriod(val label: String) {
    ALL("Tümü"),
    LAST_7_DAYS("Son 7 gün"),
    LAST_30_DAYS("Son 30 gün"),
    CUSTOM("Özel"),
}

enum class RecordEditorMode {
    CREATE,
    EDIT,
    DUPLICATE,
}

data class RecordFilters(
    val period: RecordPeriod = RecordPeriod.ALL,
    val productId: String? = null,
    val trigger: String? = null,
    val mood: String? = null,
    val unknownOnly: Boolean = false,
    val notesOnly: Boolean = false,
    val startDate: String = "",
    val endDate: String = "",
) {
    val activeCount: Int
        get() = listOf(
            period != RecordPeriod.ALL,
            productId != null,
            trigger != null,
            mood != null,
            unknownOnly,
            notesOnly,
        ).count { it }
}

data class RecordEditorState(
    val mode: RecordEditorMode,
    val source: SmokingRecordEntity?,
    val date: String,
    val time: String,
    val productId: String,
    val quantity: Int = 1,
    val consumedQuarter: Int = 4,
    val cravingLevel: Int? = null,
    val trigger: String? = null,
    val mood: String? = null,
    val locationType: String? = null,
    val note: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

data class RecordsUiState(
    val query: String = "",
    val records: List<SmokingRecordEntity> = emptyList(),
    val totalCount: Int = 0,
    val products: List<CigaretteProductEntity> = emptyList(),
    val selectedRecord: SmokingRecordEntity? = null,
    val editor: RecordEditorState? = null,
    val filters: RecordFilters = RecordFilters(),
    val filtersVisible: Boolean = false,
)

private data class RecordsInteraction(
    val selectedRecordId: String? = null,
    val editor: RecordEditorState? = null,
    val filtersVisible: Boolean = false,
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val clock: Clock,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(RecordFilters())
    private val interaction = MutableStateFlow(RecordsInteraction())
    private val deletedRecords = Channel<SmokingRecordEntity>(Channel.BUFFERED)
    private val messages = Channel<String>(Channel.BUFFERED)
    val deletions = deletedRecords.receiveAsFlow()
    val notifications = messages.receiveAsFlow()

    val state: StateFlow<RecordsUiState> = combine(
        repository.observeAllRecords(),
        repository.observeAllProducts(),
        query,
        filters,
        interaction,
    ) { records, products, search, activeFilters, interactionState ->
        val filtered = records.filter { record ->
            matchesSearch(record, search) &&
                matchesFilters(record, activeFilters)
        }
        RecordsUiState(
            query = search,
            records = filtered,
            totalCount = filtered.sumOf { it.quantity },
            products = products,
            selectedRecord = records.firstOrNull { it.id == interactionState.selectedRecordId },
            editor = interactionState.editor,
            filters = activeFilters,
            filtersVisible = interactionState.filtersVisible,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun showFilters() {
        interaction.value = interaction.value.copy(filtersVisible = true)
    }

    fun hideFilters() {
        interaction.value = interaction.value.copy(filtersVisible = false)
    }

    fun updatePeriod(value: RecordPeriod) {
        filters.value = filters.value.copy(period = value)
    }

    fun updateStartDate(value: String) {
        filters.value = filters.value.copy(period = RecordPeriod.CUSTOM, startDate = value)
    }

    fun updateEndDate(value: String) {
        filters.value = filters.value.copy(period = RecordPeriod.CUSTOM, endDate = value)
    }

    fun updateProductFilter(value: String?) {
        filters.value = filters.value.copy(productId = value)
    }

    fun updateTriggerFilter(value: String?) {
        filters.value = filters.value.copy(trigger = value)
    }

    fun updateMoodFilter(value: String?) {
        filters.value = filters.value.copy(mood = value)
    }

    fun updateUnknownFilter(value: Boolean) {
        filters.value = filters.value.copy(unknownOnly = value)
    }

    fun updateNotesFilter(value: Boolean) {
        filters.value = filters.value.copy(notesOnly = value)
    }

    fun clearFilters() {
        filters.value = RecordFilters()
    }

    fun select(record: SmokingRecordEntity) {
        interaction.value = interaction.value.copy(selectedRecordId = record.id)
    }

    fun dismissDetail() {
        interaction.value = interaction.value.copy(selectedRecordId = null)
    }

    fun openCreate() {
        val now = LocalDateTime.now(clock)
        val defaultProduct = state.value.products.firstOrNull { it.isDefault && !it.isArchived }
            ?: state.value.products.firstOrNull { !it.isArchived }
        interaction.value = interaction.value.copy(
            selectedRecordId = null,
            editor = RecordEditorState(
                mode = RecordEditorMode.CREATE,
                source = null,
                date = now.toLocalDate().toString(),
                time = now.toLocalTime().format(TIME_FORMAT),
                productId = defaultProduct?.id.orEmpty(),
            ),
        )
    }

    fun openEdit(record: SmokingRecordEntity) {
        interaction.value = interaction.value.copy(
            selectedRecordId = null,
            editor = editorFrom(record, RecordEditorMode.EDIT, record.smokedAtEpochMillis),
        )
    }

    fun openDuplicate(record: SmokingRecordEntity) {
        interaction.value = interaction.value.copy(
            selectedRecordId = null,
            editor = editorFrom(record, RecordEditorMode.DUPLICATE, clock.millis()),
        )
    }

    fun dismissEditor() {
        interaction.value = interaction.value.copy(editor = null)
    }

    fun updateEditor(transform: (RecordEditorState) -> RecordEditorState) {
        val editor = interaction.value.editor ?: return
        interaction.value = interaction.value.copy(editor = transform(editor).copy(error = null))
    }

    fun saveEditor() {
        val editor = interaction.value.editor ?: return
        if (editor.isSaving) return
        val smokedAt = parseDateTime(editor.date, editor.time)
        val error = when {
            editor.productId.isBlank() -> "Lütfen bir ürün seç."
            editor.quantity !in 1..99 -> "Adet 1–99 arasında olmalı."
            editor.consumedQuarter !in 1..4 -> "İçilen oran geçersiz."
            editor.cravingLevel != null && editor.cravingLevel !in 1..5 ->
                "İstek seviyesi 1–5 arasında olmalı."
            smokedAt == null -> "Tarih ve saati YYYY-AA-GG / SS:DD biçiminde gir."
            smokedAt > clock.millis() -> "Gelecek zamana kayıt eklenemez."
            else -> null
        }
        if (error != null) {
            updateEditor { it.copy(error = error) }
            return
        }
        val draft = SmokingRecordDraft(
            productId = editor.productId,
            smokedAtEpochMillis = checkNotNull(smokedAt),
            quantity = editor.quantity,
            consumedQuarter = editor.consumedQuarter,
            cravingLevel = editor.cravingLevel,
            trigger = editor.trigger,
            mood = editor.mood,
            locationType = editor.locationType,
            note = editor.note,
        )
        updateEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            val saved = if (editor.mode == RecordEditorMode.EDIT && editor.source != null) {
                repository.updateRecord(editor.source, draft)
            } else {
                repository.createRecord(draft)
            }
            if (saved == null) {
                updateEditor { it.copy(isSaving = false, error = "Seçilen ürün artık bulunamıyor.") }
            } else {
                interaction.value = interaction.value.copy(editor = null)
                messages.send(
                    if (editor.mode == RecordEditorMode.EDIT) "Kayıt güncellendi"
                    else "Kayıt eklendi",
                )
            }
        }
    }

    fun delete(record: SmokingRecordEntity) {
        viewModelScope.launch {
            repository.undoRecord(record.id)
            interaction.value = interaction.value.copy(selectedRecordId = null)
            deletedRecords.send(record)
        }
    }

    fun restore(record: SmokingRecordEntity) {
        viewModelScope.launch { repository.restoreRecord(record) }
    }

    private fun editorFrom(
        record: SmokingRecordEntity,
        mode: RecordEditorMode,
        timestamp: Long,
    ): RecordEditorState {
        val local = Instant.ofEpochMilli(timestamp).atZone(clock.zone).toLocalDateTime()
        return RecordEditorState(
            mode = mode,
            source = record,
            date = local.toLocalDate().toString(),
            time = local.toLocalTime().format(TIME_FORMAT),
            productId = record.productId.orEmpty(),
            quantity = record.quantity,
            consumedQuarter = record.consumedQuarter,
            cravingLevel = record.cravingLevel,
            trigger = record.trigger,
            mood = record.mood,
            locationType = record.locationType,
            note = record.note.orEmpty(),
        )
    }

    private fun matchesSearch(record: SmokingRecordEntity, search: String): Boolean =
        search.isBlank() ||
            record.productNameSnapshot.contains(search, ignoreCase = true) ||
            record.note.orEmpty().contains(search, ignoreCase = true) ||
            record.trigger.orEmpty().contains(search, ignoreCase = true) ||
            record.mood.orEmpty().contains(search, ignoreCase = true)

    private fun matchesFilters(record: SmokingRecordEntity, value: RecordFilters): Boolean {
        val cutoff = when (value.period) {
            RecordPeriod.ALL -> null
            RecordPeriod.LAST_7_DAYS -> LocalDate.now(clock).minusDays(6)
            RecordPeriod.LAST_30_DAYS -> LocalDate.now(clock).minusDays(29)
            RecordPeriod.CUSTOM -> value.startDate.toLocalDateOrNull()
        }
        val endDate = if (value.period == RecordPeriod.CUSTOM) {
            value.endDate.toLocalDateOrNull()
        } else {
            null
        }
        val recordDate = Instant.ofEpochMilli(record.smokedAtEpochMillis)
            .atZone(clock.zone)
            .toLocalDate()
        return (cutoff == null || !recordDate.isBefore(cutoff)) &&
            (endDate == null || !recordDate.isAfter(endDate)) &&
            (value.productId == null || record.productId == value.productId) &&
            (value.trigger == null || record.trigger == value.trigger) &&
            (value.mood == null || record.mood == value.mood) &&
            (!value.notesOnly || !record.note.isNullOrBlank()) &&
            (!value.unknownOnly ||
                record.productId == null ||
                record.priceMicrosPerCigaretteSnapshot == null ||
                record.nicotineMicrogramsPerCigaretteSnapshot == null ||
                record.tarMicrogramsPerCigaretteSnapshot == null ||
                record.carbonMonoxideMicrogramsPerCigaretteSnapshot == null)
    }

    private fun parseDateTime(date: String, time: String): Long? = runCatching {
        LocalDateTime.of(LocalDate.parse(date.trim()), LocalTime.parse(time.trim(), TIME_FORMAT))
            .atZone(clock.zone)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    companion object {
        val TRIGGERS = listOf(
            "Kahve",
            "Yemek sonrası",
            "Stres",
            "Sosyal ortam",
            "Alkol",
            "Can sıkıntısı",
            "Çalışma molası",
            "Telefon",
            "Araç kullanma",
            "Alışkanlık",
            "Diğer",
        )
        val MOODS = listOf("Sakin", "Mutlu", "Yorgun", "Gergin", "Üzgün", "Odaklanmış", "Diğer")
        val LOCATIONS = listOf("Ev", "İş", "Dışarı", "Araç", "Sosyal alan", "Diğer")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    }
}

private fun String.toLocalDateOrNull(): LocalDate? =
    takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
