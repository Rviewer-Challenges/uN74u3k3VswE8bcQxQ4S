package com.example.firebasechat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.firebasechat.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = OffWhite,
    surface = Black,
    background = OffBlack,
    primaryContainer = Black,
    secondary = DarkGray,
    onSecondary = OffWhite,
    onPrimary = Black
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    surface = White,
    background = OffWhite,
    primaryContainer = White,
    secondary = LightGray,
    onSecondary = Black,
    onPrimary = White
)

@Composable
fun FirebaseChatTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.System -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        ThemeMode.Light -> LightColorScheme
        ThemeMode.Dark -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
        shapes = Shapes
    )
}