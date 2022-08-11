package com.example.firebasechat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatVM @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn

    init {
        viewModelScope.launch {
            authManager.authState.map { newAuthState ->
                newAuthState is AuthState.SignedIn
            }.collectLatest { newSignedIn ->
                _signedIn.value = newSignedIn
            }
        }
    }

    fun onUIEvent(event: ChatUIEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                ChatUIEvent.signIn -> authManager.signIn()
                ChatUIEvent.signOut -> authManager.signOut()
            }
        }
    }
}