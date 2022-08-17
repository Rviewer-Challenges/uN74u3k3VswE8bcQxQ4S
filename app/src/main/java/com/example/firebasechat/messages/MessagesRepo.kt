package com.example.firebasechat.messages

import com.example.firebasechat.BuildConfig
import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.auth.model.User
import com.example.firebasechat.auth.model.UserSnapshot
import com.example.firebasechat.auth.model.toUser
import com.example.firebasechat.di.Modules
import com.example.firebasechat.messages.model.Message
import com.example.firebasechat.messages.model.MessageSnapshot
import com.example.firebasechat.messages.model.toMessage
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

    private val firebaseUsers = Firebase.database(BuildConfig.FIREBASE_URL).reference.child("users")
    private val firebaseMessages = Firebase.database(BuildConfig.FIREBASE_URL).reference.child("messages")

    private var _uidsAlreadyAdded = mutableSetOf<String>()
    private var users = mutableMapOf<String, User>()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages

    private val messagesListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val newMessageSnapshot = snapshot.getValue<MessageSnapshot>() ?: return
            val uid = snapshot.key!!
            if (uid in _uidsAlreadyAdded) return
            _uidsAlreadyAdded.add(uid)

            val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
            val newMessage = newMessageSnapshot.toMessage(
                uid = uid,
                user = users[newMessageSnapshot.authorUid],
                isSelf = userUid != null && userUid == newMessageSnapshot.authorUid
            )
            _messages.value = messages.value + newMessage
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            logcat { "MESSAGE_TEST, onChildChanged 1" }
            // TODO reactions?
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {
            logcat { "MESSAGE_TEST, on message listener cancelled" }
        }
    }

    private val usersListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val newUserSnapshot = snapshot.getValue<UserSnapshot>() ?: return
            val uid = snapshot.key!!
            if (uid in users) return

            val newUser = newUserSnapshot.toUser(uid)
            users[uid] = newUser
            // TODO update messages?
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val changedUserSnapshot = snapshot.getValue<UserSnapshot>() ?: return
            val uid = snapshot.key!!
            val changedUser = changedUserSnapshot.toUser(uid)
            users[uid] = changedUser
            // TODO update messages?
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {
            logcat { "MESSAGE_TEST, on user listener cancelled" }
        }
    }

        init {
            initData()
            observeAuth()
        }

    private fun initData() {
        // Load messages
        firebaseUsers.get().addOnSuccessListener { usersSnapshot ->
            val initialUsers = usersSnapshot.children.map { it.key!! to it.getValue<UserSnapshot>()!! }
            setInitialUsers(initialUsers)
            // Now load messages
            firebaseMessages.get().addOnSuccessListener { messagesSnapshot ->
                val initialMessages = messagesSnapshot.children.map { it.key!! to it.getValue<MessageSnapshot>()!! }
                if (authManager.authState.value is AuthState.SignedIn) {
                    val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
                    setInitialMessages(initialMessages, userUid)
                } else {
                    setInitialMessages(initialMessages)
                }
            }.addOnFailureListener {
                // TODO
                logcat { "MESSAGE_TEST, failed to load initial messages - notify the user" }
            }
        }.addOnFailureListener {
            // TODO
            logcat { "MESSAGE_TEST, failed to load initial users - notify the user" }
        }
    }

    private fun setInitialUsers(initialUsers: List<Pair<String, UserSnapshot>>) {
        users = initialUsers.associate { user -> user.first to user.second.toUser(user.first) }.toMutableMap()
        firebaseUsers.addChildEventListener(usersListener)
    }

    private fun setInitialMessages(initialMessages: List<Pair<String, MessageSnapshot>>, userUid: String? = null) {
        _uidsAlreadyAdded.addAll(initialMessages.map { it.first })
        _messages.value = initialMessages.map { message ->
            message.second.toMessage(
                uid = message.first,
                user = users[message.second.authorUid],
                isSelf = userUid != null && userUid == message.second.authorUid
            )
        }
        firebaseMessages.addChildEventListener(messagesListener)
    }

    private fun observeAuth() {
        externalScope.launch(dispatcherDefault) {
            var previous: AuthState? = null
            authManager.authState.collectLatest { authState ->
                when {
                    authState is AuthState.SignedIn && previous !is AuthState.SignedIn -> {
                        _messages.value = messages.value.map { it.copy(isSelf = authState.user.uid == it.user?.uid) }
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
        messageRef.setValue(MessageSnapshot(content, userToken))
    }
}