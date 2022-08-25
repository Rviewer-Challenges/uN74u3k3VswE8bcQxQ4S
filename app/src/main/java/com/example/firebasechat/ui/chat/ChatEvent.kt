package com.example.firebasechat.ui.chat

sealed class ChatUIEvent {
    object SwapDarkLightMode : ChatUIEvent()
    object SignIn : ChatUIEvent()
    object SignOut : ChatUIEvent()

    data class OnEditorChanged(val newMessage: String) : ChatUIEvent()
    object OnMessageSent : ChatUIEvent()
    data class OnReactionPressed(val emoji: String, val messageUid: String) : ChatUIEvent()
}