package com.umityasincoban.nefesizi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.AppPreferences
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.core.data.PersonalizationPreferences
import com.umityasincoban.nefesizi.core.security.AppLockSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppState {
    data object Loading : AppState
    data object Onboarding : AppState
    data object Ready : AppState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    preferences: AppPreferences,
) : ViewModel() {
    val appLockSession = AppLockSession()

    val state: StateFlow<AppState> = preferences.onboardingCompleted
        .map { completed -> if (completed) AppState.Ready else AppState.Onboarding }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Loading)

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val personalization: StateFlow<PersonalizationPreferences> = preferences.personalization
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PersonalizationPreferences(),
        )
}
