package com.umityasincoban.nefesizi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.AppPreferences
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.core.data.TodayDisplayPreferences
import com.umityasincoban.nefesizi.core.data.PersonalizationPreferences
import com.umityasincoban.nefesizi.core.data.NotificationPreferences
import com.umityasincoban.nefesizi.core.notification.NotificationScheduler
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.first
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
    val personalization: PersonalizationPreferences = PersonalizationPreferences(),
    val notifications: NotificationPreferences = NotificationPreferences(),
    val biometricSupported: Boolean = false,
)

private data class SettingsBase(
    val theme: ThemeMode,
    val products: List<CigaretteProductEntity>,
    val todayDisplay: TodayDisplayPreferences,
    val personalization: PersonalizationPreferences,
    val notifications: NotificationPreferences,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: NefesIziRepository,
    private val preferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {
    private val wakeTimeDraft = MutableStateFlow<String?>(null)

    private val base = combine(
        preferences.themeMode,
        repository.observeProducts(),
        preferences.todayDisplayPreferences,
        preferences.personalization,
        preferences.notifications,
    ) { theme, products, todayDisplay, personalization, notifications ->
        SettingsBase(theme, products, todayDisplay, personalization, notifications)
    }

    val state: StateFlow<SettingsUiState> = combine(
        base,
        preferences.wakeTime,
        wakeTimeDraft,
    ) { base, wakeTime, draft ->
        SettingsUiState(
            base.theme,
            base.products,
            base.todayDisplay,
            draft ?: wakeTime.orEmpty(),
            base.personalization,
            base.notifications,
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            ) == BiometricManager.BIOMETRIC_SUCCESS,
        )
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

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColor(enabled) }
    }

    fun setCurrency(code: String) {
        val normalized = code.trim().uppercase().take(3)
        if (runCatching { java.util.Currency.getInstance(normalized) }.isSuccess) {
            viewModelScope.launch { preferences.setPreferredCurrency(normalized) }
        }
    }

    fun setDayStartHour(hour: Int) {
        viewModelScope.launch { preferences.setDayStartHour(hour) }
    }

    fun setFirstDayOfWeek(value: String) {
        viewModelScope.launch { preferences.setFirstDayOfWeek(value) }
    }

    fun setShowHealthTab(show: Boolean) {
        viewModelScope.launch { preferences.setShowHealthTab(show) }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { preferences.setBiometricLock(enabled) }
    }

    fun setEveningNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setEveningNotification(enabled)
            notificationScheduler.sync(preferences.notifications.first())
        }
    }

    fun setWeeklyNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setWeeklyNotification(enabled)
            notificationScheduler.sync(preferences.notifications.first())
        }
    }

    fun setInactivityNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setInactivityNotification(enabled)
            notificationScheduler.sync(preferences.notifications.first())
        }
    }

    fun setEveningTime(value: String) {
        if (runCatching { java.time.LocalTime.parse(value) }.isSuccess) {
            viewModelScope.launch {
                preferences.setEveningTime(value)
                notificationScheduler.sync(preferences.notifications.first())
            }
        }
    }
}
