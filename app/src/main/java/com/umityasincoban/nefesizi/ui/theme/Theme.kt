package com.umityasincoban.nefesizi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9A7E),
    onPrimary = Midnight,
    primaryContainer = Color(0xFF713424),
    onPrimaryContainer = SoftCoral,
    secondary = Color(0xFF9CCFD0),
    onSecondary = Midnight,
    secondaryContainer = DeepTeal,
    onSecondaryContainer = Mist,
    tertiary = Amber,
    background = Midnight,
    onBackground = Color(0xFFF1F5F2),
    surface = NightSurface,
    onSurface = Color(0xFFF1F5F2),
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = Color(0xFFC2D0CE),
    outline = Color(0xFF668086),
)

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = SoftCoral,
    onPrimaryContainer = Color(0xFF4A160A),
    secondary = DeepTeal,
    onSecondary = Color.White,
    secondaryContainer = Mist,
    onSecondaryContainer = Midnight,
    tertiary = Amber,
    background = Parchment,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EFEC),
    onSurfaceVariant = Slate,
    outline = Color(0xFF879593),
)

@Composable
fun NefesIziTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colors,
        typography = NefesTypography,
        content = content,
    )
}
