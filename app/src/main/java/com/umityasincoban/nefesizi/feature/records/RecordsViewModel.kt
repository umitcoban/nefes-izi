package com.umityasincoban.nefesizi.feature.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordsUiState(
    val query: String = "",
    val records: List<SmokingRecordEntity> = emptyList(),
    val totalCount: Int = 0,
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repository: NefesIziRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val deletedRecords = Channel<SmokingRecordEntity>(Channel.BUFFERED)
    val deletions = deletedRecords.receiveAsFlow()

    val state: StateFlow<RecordsUiState> = combine(
        repository.observeAllRecords(),
        query,
    ) { records, search ->
        val filtered = if (search.isBlank()) {
            records
        } else {
            records.filter {
                it.productNameSnapshot.contains(search, ignoreCase = true) ||
                    it.note.orEmpty().contains(search, ignoreCase = true)
            }
        }
        RecordsUiState(
            query = search,
            records = filtered,
            totalCount = records.sumOf { it.quantity },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun delete(record: SmokingRecordEntity) {
        viewModelScope.launch {
            repository.undoRecord(record.id)
            deletedRecords.send(record)
        }
    }

    fun restore(record: SmokingRecordEntity) {
        viewModelScope.launch { repository.restoreRecord(record) }
    }
}
