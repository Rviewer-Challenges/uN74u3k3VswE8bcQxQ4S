package com.example.firebasechat.ui.chat

import androidx.compose.runtime.Immutable
import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.messages.model.Message
import com.example.firebasechat.settings.ThemeMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChatState(
    val authState: AuthState = AuthState.Unknown,
    val editor: String = "",
    val messages: ImmutableList<Message> = persistentListOf(),
    val themeMode: ThemeMode = ThemeMode.Default
)