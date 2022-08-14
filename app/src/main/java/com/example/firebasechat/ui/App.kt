package com.example.firebasechat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.example.firebasechat.ui.chat.ChatScreen
import com.example.firebasechat.ui.theme.FirebaseChatTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun App() {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    FirebaseChatTheme {
        ChatScreen()
    }
}