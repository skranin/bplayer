package com.bplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = BrandRed,
    onPrimary = OnDark,
    secondary = BrandRedSoft,
    background = DarkBg,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkMuted,
    outline = DarkOutline,
)

private val LightColors = lightColorScheme(
    primary = BrandRedDeep,
    onPrimary = LightSurface,
    secondary = BrandRed,
    background = LightBg,
    onBackground = OnLight,
    surface = LightSurface,
    onSurface = OnLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightMuted,
    outline = LightOutline,
)

@Composable
fun BPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
