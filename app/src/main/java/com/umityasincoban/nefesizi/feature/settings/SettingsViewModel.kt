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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val products: List<CigaretteProductEntity> = emptyList(),
    val todayDisplay: TodayDisplayPreferences = TodayDisplayPreferences(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val preferences: AppPreferences,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        preferences.themeMode,
        repository.observeProducts(),
        preferences.todayDisplayPreferences,
    ) { theme, products, todayDisplay ->
        SettingsUiState(theme, products, todayDisplay)
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
}
