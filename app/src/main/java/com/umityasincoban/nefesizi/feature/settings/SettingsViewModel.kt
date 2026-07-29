package com.umityasincoban.nefesizi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.AppPreferences
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.core.data.TodayDisplayPreferences
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val products: List<CigaretteProductEntity> = emptyList(),
    val todayDisplay: TodayDisplayPreferences = TodayDisplayPreferences(),
    val wakeTime: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val wakeTimeDraft = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        preferences.themeMode,
        repository.observeProducts(),
        preferences.todayDisplayPreferences,
        preferences.wakeTime,
        wakeTimeDraft,
    ) { theme, products, todayDisplay, wakeTime, draft ->
        SettingsUiState(theme, products, todayDisplay, draft ?: wakeTime.orEmpty())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setDefaultProduct(product: CigaretteProductEntity) {
        viewModelScope.launch { repository.setDefaultProduct(product) }
    }

    fun setShowTodayCost(show: Boolean) {
        viewModelScope.launch { preferences.setShowTodayCost(show) }
    }

    fun setShowTodayExposure(show: Boolean) {
        viewModelScope.launch { preferences.setShowTodayExposure(show) }
    }

    fun setWakeTime(value: String) {
        val normalized = value.trim().take(5)
        wakeTimeDraft.value = normalized
        if (normalized.isBlank() || runCatching { java.time.LocalTime.parse(normalized) }.isSuccess) {
            viewModelScope.launch {
                preferences.setWakeTime(normalized.takeIf(String::isNotBlank))
            }
        }
    }
}
