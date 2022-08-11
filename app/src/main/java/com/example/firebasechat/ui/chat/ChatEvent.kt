package com.example.firebasechat.ui.chat

sealed class ChatUIEvent {
    object signIn: ChatUIEvent()
    object signOut: ChatUIEvent()
}