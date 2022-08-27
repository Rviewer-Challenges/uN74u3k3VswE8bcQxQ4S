package com.example.firebasechat.ui.chat

import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.messages.model.Message
import com.example.firebasechat.settings.ThemeMode

data class ChatState(
    val authState: AuthState = AuthState.Unknown,
    val editor: String = "",
    val messages: List<Message> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.Default
)