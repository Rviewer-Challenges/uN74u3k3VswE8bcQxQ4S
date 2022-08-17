package com.example.firebasechat.ui.chat

import com.example.firebasechat.messages.model.Message

data class ChatState(
    val isSignedIn: Boolean = false,
    val isDarkMode: Boolean = false,

    val editor: String = "",
    val messages: List<Message> = emptyList()
)