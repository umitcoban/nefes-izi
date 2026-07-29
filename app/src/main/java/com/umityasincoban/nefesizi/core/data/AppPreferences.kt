package com.umityasincoban.nefesizi.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nefes_izi_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED] ?: false }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
    }

    val todayDisplayPreferences: Flow<TodayDisplayPreferences> = context.dataStore.data
        .map { preferences ->
            TodayDisplayPreferences(
                showCost = preferences[SHOW_TODAY_COST] ?: true,
                showExposure = preferences[SHOW_TODAY_EXPOSURE] ?: true,
            )
        }

    val wakeTime: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[WAKE_TIME]?.takeIf(String::isNotBlank) }

    val personalization: Flow<PersonalizationPreferences> = context.dataStore.data
        .map { preferences ->
            PersonalizationPreferences(
                dynamicColor = preferences[DYNAMIC_COLOR] ?: false,
                preferredCurrency = preferences[PREFERRED_CURRENCY] ?: "TRY",
                dayStartHour = preferences[DAY_START_HOUR] ?: 0,
                firstDayOfWeek = preferences[FIRST_DAY_OF_WEEK] ?: "MONDAY",
                showHealthTab = preferences[SHOW_HEALTH_TAB] ?: true,
                biometricLock = preferences[BIOMETRIC_LOCK] ?: false,
            )
        }

    val notifications: Flow<NotificationPreferences> = context.dataStore.data
        .map { preferences ->
            NotificationPreferences(
                eveningEnabled = preferences[EVENING_ENABLED] ?: false,
                eveningTime = preferences[EVENING_TIME] ?: "21:00",
                weeklyEnabled = preferences[WEEKLY_ENABLED] ?: false,
                inactivityEnabled = preferences[INACTIVITY_ENABLED] ?: false,
                inactivityDays = preferences[INACTIVITY_DAYS] ?: 3,
            )
        }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setShowTodayCost(show: Boolean) {
        context.dataStore.edit { it[SHOW_TODAY_COST] = show }
    }

    suspend fun setShowTodayExposure(show: Boolean) {
        context.dataStore.edit { it[SHOW_TODAY_EXPOSURE] = show }
    }

    suspend fun setWakeTime(value: String?) {
        context.dataStore.edit { preferences ->
            if (value.isNullOrBlank()) {
                preferences.remove(WAKE_TIME)
            } else {
                preferences[WAKE_TIME] = value
            }
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setPreferredCurrency(code: String) {
        context.dataStore.edit { it[PREFERRED_CURRENCY] = code }
    }

    suspend fun setDayStartHour(hour: Int) {
        context.dataStore.edit { it[DAY_START_HOUR] = hour.coerceIn(0, 23) }
    }

    suspend fun setFirstDayOfWeek(value: String) {
        context.dataStore.edit { it[FIRST_DAY_OF_WEEK] = value }
    }

    suspend fun setShowHealthTab(show: Boolean) {
        context.dataStore.edit { it[SHOW_HEALTH_TAB] = show }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setEveningNotification(enabled: Boolean) {
        context.dataStore.edit { it[EVENING_ENABLED] = enabled }
    }

    suspend fun setEveningTime(value: String) {
        context.dataStore.edit { it[EVENING_TIME] = value }
    }

    suspend fun setWeeklyNotification(enabled: Boolean) {
        context.dataStore.edit { it[WEEKLY_ENABLED] = enabled }
    }

    suspend fun setInactivityNotification(enabled: Boolean) {
        context.dataStore.edit { it[INACTIVITY_ENABLED] = enabled }
    }

    suspend fun setInactivityDays(days: Int) {
        context.dataStore.edit { it[INACTIVITY_DAYS] = days.coerceIn(1, 30) }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_TODAY_COST = booleanPreferencesKey("show_today_cost")
        val SHOW_TODAY_EXPOSURE = booleanPreferencesKey("show_today_exposure")
        val WAKE_TIME = stringPreferencesKey("wake_time")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency")
        val DAY_START_HOUR = intPreferencesKey("day_start_hour")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val SHOW_HEALTH_TAB = booleanPreferencesKey("show_health_tab")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val EVENING_ENABLED = booleanPreferencesKey("notification_evening_enabled")
        val EVENING_TIME = stringPreferencesKey("notification_evening_time")
        val WEEKLY_ENABLED = booleanPreferencesKey("notification_weekly_enabled")
        val INACTIVITY_ENABLED = booleanPreferencesKey("notification_inactivity_enabled")
        val INACTIVITY_DAYS = intPreferencesKey("notification_inactivity_days")
    }
}

data class TodayDisplayPreferences(
    val showCost: Boolean = true,
    val showExposure: Boolean = true,
)

data class PersonalizationPreferences(
    val dynamicColor: Boolean = false,
    val preferredCurrency: String = "TRY",
    val dayStartHour: Int = 0,
    val firstDayOfWeek: String = "MONDAY",
    val showHealthTab: Boolean = true,
    val biometricLock: Boolean = false,
)

data class NotificationPreferences(
    val eveningEnabled: Boolean = false,
    val eveningTime: String = "21:00",
    val weeklyEnabled: Boolean = false,
    val inactivityEnabled: Boolean = false,
    val inactivityDays: Int = 3,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
