package com.example.firebasechat.ui

import androidx.compose.runtime.Composable
import com.example.firebasechat.ui.chat.ChatScreen
import com.example.firebasechat.ui.theme.FirebaseChatTheme

@Composable
fun App() {
    // TODO insets
    FirebaseChatTheme {
        ChatScreen()
    }
}