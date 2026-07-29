package com.umityasincoban.nefesizi

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import android.os.SystemClock
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.ui.AppViewModel
import com.umityasincoban.nefesizi.ui.NefesIziRoot
import com.umityasincoban.nefesizi.ui.theme.NefesIziTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val locked = mutableStateOf(false)
    private var lockEnabled = false
    private var authenticatedThisSession = false
    private var lockPreferenceLoaded = false
    private var backgroundAtElapsed = 0L
    private lateinit var lockPrompt: BiometricPrompt
    private lateinit var lockPromptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lockPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    authenticatedThisSession = true
                    locked.value = false
                }
            },
        )
        lockPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Nefes İzi kilitli")
            .setSubtitle("Uygulamaya devam etmek için doğrula")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        lifecycleScope.launch {
            appViewModel.personalization.collect { personalization ->
                val wasEnabled = lockEnabled
                lockEnabled = personalization.biometricLock
                if (!lockPreferenceLoaded) {
                    lockPreferenceLoaded = true
                } else if (!wasEnabled && lockEnabled) {
                    // Ayar yalnızca Settings ekranındaki başarılı doğrulamadan sonra açılır.
                    authenticatedThisSession = true
                }
                if (lockEnabled && !authenticatedThisSession && !locked.value) {
                    showLock()
                }
                if (!lockEnabled) locked.value = false
            }
        }
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val personalization by appViewModel.personalization.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            NefesIziTheme(
                darkTheme = darkTheme,
                dynamicColor = personalization.dynamicColor,
            ) {
                if (locked.value) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("Nefes İzi kilitli")
                            Button(onClick = ::showLock) { Text("Kilidi aç") }
                        }
                    }
                } else {
                    NefesIziRoot(viewModel = appViewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (lockEnabled && backgroundAtElapsed > 0L &&
            SystemClock.elapsedRealtime() - backgroundAtElapsed >= LOCK_AFTER_MILLIS
        ) {
            authenticatedThisSession = false
            showLock()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundAtElapsed = SystemClock.elapsedRealtime()
    }

    private fun showLock() {
        locked.value = true
        lockPrompt.authenticate(lockPromptInfo)
    }

    private companion object {
        const val LOCK_AFTER_MILLIS = 30_000L
    }
}
