package com.example.firebasechat.ui.chat

import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.messages.model.Message

data class ChatState(
    val authState: AuthState = AuthState.Unknown,
    val isDarkMode: Boolean = false,

    val editor: String = "",
    val messages: List<Message> = emptyList()
)