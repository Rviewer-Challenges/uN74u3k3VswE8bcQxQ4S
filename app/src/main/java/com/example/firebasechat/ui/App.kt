package com.example.firebasechat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import com.example.firebasechat.settings.ThemeMode
import com.example.firebasechat.settings.UserSettings
import com.example.firebasechat.ui.chat.ChatScreen
import com.example.firebasechat.ui.theme.FirebaseChatTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun App(userSettings: State<UserSettings>) {
    val systemUiController = rememberSystemUiController()
    val isSystemInDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(userSettings.value.themeMode, isSystemInDarkTheme) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = when (userSettings.value.themeMode) {
                ThemeMode.System -> !isSystemInDarkTheme
                ThemeMode.Light -> true
                ThemeMode.Dark -> false
            }
        )
    }

    FirebaseChatTheme(userSettings.value.themeMode) {
        ChatScreen()
    }
}