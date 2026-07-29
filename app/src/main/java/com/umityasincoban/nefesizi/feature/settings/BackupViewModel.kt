package com.umityasincoban.nefesizi.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.backup.BackupManager
import com.umityasincoban.nefesizi.core.backup.ImportMode
import com.umityasincoban.nefesizi.core.backup.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val preview: ImportPreview? = null,
    val isWorking: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val manager: BackupManager,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()
    private val messages = Channel<String>(Channel.BUFFERED)
    val effects = messages.receiveAsFlow()

    fun exportJson(uri: Uri) = work("JSON yedeği oluşturuldu") {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
            it.write(manager.createJson())
        } ?: error("Dosya açılamadı.")
    }

    fun exportCsv(uri: Uri) = work("CSV arşivi oluşturuldu") {
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(manager.createCsvZip())
        } ?: error("Dosya açılamadı.")
    }

    fun previewImport(uri: Uri) = work(null) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Dosya okunamadı.")
        _state.update { it.copy(preview = manager.preview(text)) }
    }

    fun applyImport(mode: ImportMode) = work("Yedek içe aktarıldı") {
        manager.import(checkNotNull(_state.value.preview), mode)
        _state.update { it.copy(preview = null) }
    }

    fun dismissPreview() {
        _state.update { it.copy(preview = null) }
    }

    private fun work(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            runCatching { block() }
                .onSuccess { successMessage?.let { messages.send(it) } }
                .onFailure { messages.send(it.message ?: "İşlem tamamlanamadı.") }
            _state.update { it.copy(isWorking = false) }
        }
    }
}
