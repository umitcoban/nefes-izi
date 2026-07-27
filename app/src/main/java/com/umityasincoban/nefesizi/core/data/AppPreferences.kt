package com.umityasincoban.nefesizi.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
