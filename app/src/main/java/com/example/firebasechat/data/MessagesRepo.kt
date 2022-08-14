package com.example.firebasechat.data

import com.example.firebasechat.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject

data class Message(
    val id: String,
    val text: String,
    val author: String,
    val isSelf: Boolean
)

interface MessageRepo {
    val messages: StateFlow<List<Message>>
    suspend fun sendMessage(newMessage: String)
}

class MessagesRepoImpl @Inject constructor(
    private val authManager: AuthManager
) : MessageRepo {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages

    override suspend fun sendMessage(newMessage: String) {
        _messages.value = messages.value + Message(UUID.randomUUID().toString(), newMessage, "author", Random().nextBoolean())
    }
}