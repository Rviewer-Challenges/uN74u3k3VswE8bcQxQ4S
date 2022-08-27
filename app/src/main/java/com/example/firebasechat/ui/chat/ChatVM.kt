package com.example.firebasechat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.messages.MessageRepo
import com.example.firebasechat.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatVM @Inject constructor(
    private val authManager: AuthManager,
    private val messageRepo: MessageRepo,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    init {
        viewModelScope.launch {
            combine(
                authManager.authState,
                messageRepo.messages,
                settingsStore.settings
            ) { authState, messages, settings ->
                state.value.copy(
                    authState = authState,
                    messages = messages,
                    themeMode = settings.themeMode
                )
            }.collectLatest { newState ->
                _state.value = newState
            }
        }
    }

    fun onUIEvent(event: ChatUIEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is ChatUIEvent.SwapDarkLightMode -> {
                    settingsStore.toggleThemeMode()
                }
                is ChatUIEvent.SignIn -> authManager.signIn()
                is ChatUIEvent.SignOut -> authManager.signOut()
                is ChatUIEvent.OnEditorChanged -> {
                    _state.value = state.value.copy(editor = event.newMessage)
                }
                is ChatUIEvent.OnMessageSent -> {
                    messageRepo.sendMessage(state.value.editor.trim())
                    _state.value = state.value.copy(editor = "")
                }
                is ChatUIEvent.OnReactionPressed -> {
                    messageRepo.toggleReaction(event.emoji, event.messageUid)
                }
            }
        }
    }
}