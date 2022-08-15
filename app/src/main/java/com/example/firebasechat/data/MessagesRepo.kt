package com.example.firebasechat.data

import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.data.models.Message
import com.example.firebasechat.data.models.MessageSnapshot
import com.example.firebasechat.data.models.toMessage
import com.example.firebasechat.di.Modules
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

interface MessageRepo {
    val messages: StateFlow<List<Message>>
    fun sendMessage(content: String)
}

class MessagesRepoImpl @Inject constructor(
    private val authManager: AuthManager,
    private val externalScope: CoroutineScope,
    @Modules.DispatcherDefault private val dispatcherDefault: CoroutineDispatcher
) : MessageRepo {

    companion object {
        const val URL = "https://fir-chat-54f7d-default-rtdb.europe-west1.firebasedatabase.app/"
    }

    private val firebaseMessages = Firebase.database(URL).reference.child("messages")

    private var _messagesAlreadyAdded = mutableSetOf<String>()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages

    private val messagesListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val newMessageSnapshot = snapshot.getValue<MessageSnapshot>() ?: return
            if (newMessageSnapshot.id in _messagesAlreadyAdded) return

            val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
            val newMessage = newMessageSnapshot.toMessage(userUid)
            _messagesAlreadyAdded.add(newMessage.id)
            _messages.value = messages.value + newMessage
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            logcat { "MESSAGE_TEST, onChildChanged 1" }
            // TODO reactions?
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(databaseError: DatabaseError) {
            logcat { "MESSAGE_TEST, onCancelled" }
        }
    }

    init {
        initMessages()
        observeAuth()
    }

    private fun initMessages() {
        firebaseMessages.get().addOnSuccessListener { messagesSnapshot ->
            val initialMessages = messagesSnapshot.children.map { it.getValue<MessageSnapshot>()!! }
            if (authManager.authState.value is AuthState.SignedIn) {
                val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
                setInitialMessages(initialMessages, userUid)
            } else {
                setInitialMessages(initialMessages)
            }
        }.addOnFailureListener {
            logcat { "MESSAGE_TEST, failed to load initial list - notify the user" }
        }
    }

    private fun setInitialMessages(initialMessages: List<MessageSnapshot>, userUid: String? = null) {
        _messagesAlreadyAdded = initialMessages.map { it.id!! }.toMutableSet()
        _messages.value = initialMessages.map { it.toMessage(userUid) }
        firebaseMessages.addChildEventListener(messagesListener)
    }

    private fun observeAuth() {
        externalScope.launch(dispatcherDefault) {
            var previous: AuthState? = null
            authManager.authState.collectLatest { authState ->
                when {
                    authState is AuthState.SignedIn && previous !is AuthState.SignedIn -> {
                        _messages.value = messages.value.map { it.copy(isSelf = authState.user.uid == it.authorUid) }
                    }
                    authState !is AuthState.SignedIn && previous is AuthState.SignedIn -> {
                        _messages.value = messages.value.map { it.copy(isSelf = false) }
                    }
                    else -> {
                        // Change between intermediate states, no need to do anything
                    }
                }
                previous = authState
            }
        }
    }

    override fun sendMessage(content: String) {
        val userToken = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid ?: return
        val messageRef = firebaseMessages.push()
        messageRef.setValue(MessageSnapshot(messageRef.key!!, content, userToken))
    }
}