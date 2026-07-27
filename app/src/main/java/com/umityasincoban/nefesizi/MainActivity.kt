package com.umityasincoban.nefesizi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.ui.AppViewModel
import com.umityasincoban.nefesizi.ui.NefesIziRoot
import com.umityasincoban.nefesizi.ui.theme.NefesIziTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            NefesIziTheme(darkTheme = darkTheme) {
                NefesIziRoot(viewModel = appViewModel)
            }
        }
    }
}
