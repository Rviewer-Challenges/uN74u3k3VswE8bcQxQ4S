package com.example.firebasechat.messages

import com.example.firebasechat.BuildConfig
import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.auth.AuthState
import com.example.firebasechat.auth.model.User
import com.example.firebasechat.auth.model.UserSnapshot
import com.example.firebasechat.auth.model.toUser
import com.example.firebasechat.di.Modules
import com.example.firebasechat.messages.model.*
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

interface MessageRepo { // TODO loading/error states
    val messages: StateFlow<PersistentList<Message>>
    fun sendMessage(content: String)
    fun toggleReaction(emoji: String, messageUid: String)
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
    private val _messages = MutableStateFlow<PersistentList<Message>>(persistentListOf())
    override val messages: StateFlow<PersistentList<Message>> = _messages

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
                isSelf = userUid != null && userUid == newMessageSnapshot.authorUid,
                reactions = getReactions(snapshot)
            )
            _messages.value = persistentListOf(newMessage) + messages.value
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val changedMessageSnapshot = snapshot.getValue<MessageSnapshot>() ?: return
            val uid = snapshot.key!!
            val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
            val changedMessage = changedMessageSnapshot.toMessage(
                uid = uid,
                user = users[changedMessageSnapshot.authorUid],
                isSelf = userUid != null && userUid == changedMessageSnapshot.authorUid,
                reactions = getReactions(snapshot)
            )

            _messages.value = messages.value.map { existingMessage ->
                if (existingMessage.uid == changedMessage.uid) changedMessage else existingMessage
            }.toPersistentList()
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
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
        override fun onCancelled(databaseError: DatabaseError) {}
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
                if (authManager.authState.value is AuthState.SignedIn) {
                    val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
                    setInitialMessages(messagesSnapshot, userUid)
                } else {
                    setInitialMessages(messagesSnapshot)
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

    private fun setInitialMessages(messagesSnapshot: DataSnapshot, userUid: String? = null) {
        _uidsAlreadyAdded.addAll(messagesSnapshot.children.map { it.key!! })
        _messages.value = messagesSnapshot.children.map { messageDataSnapshot ->
            val messageSnapshot = messageDataSnapshot.getValue<MessageSnapshot>()!!
            messageSnapshot.toMessage(
                uid = messageDataSnapshot.key!!,
                user = users[messageSnapshot.authorUid],
                isSelf = userUid != null && userUid == messageSnapshot.authorUid,
                reactions = getReactions(messageDataSnapshot)
            )
        }.reversed().toPersistentList()
        firebaseMessages.addChildEventListener(messagesListener)
    }

    private fun observeAuth() {
        externalScope.launch(dispatcherDefault) {
            var previous: AuthState? = null
            authManager.authState.collectLatest { authState ->
                when {
                    authState is AuthState.SignedIn && previous !is AuthState.SignedIn -> {
                        _messages.value = messages.value.map { original ->
                            original.copy(
                                isSelf = authState.user.uid == original.user?.uid,
                                reactions = original.reactions.map { reaction ->
                                    reaction.copy(isSelf = authState.user.uid == reaction.user?.uid)
                                }.toPersistentList()
                            )
                        }.toPersistentList()
                    }
                    authState !is AuthState.SignedIn && previous is AuthState.SignedIn -> {
                        _messages.value = messages.value.map { original ->
                            original.copy(
                                isSelf = false,
                                reactions = original.reactions.map { reaction ->
                                    reaction.copy(isSelf = false)
                                }.toPersistentList()
                            )
                        }.toPersistentList()
                    }
                    else -> {
                        // Change between intermediate states, no need to do anything
                    }
                }
                previous = authState
            }
        }
    }

    private fun getReactions(messageDataSnapshot: DataSnapshot): List<Reaction> {
        val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid
        return messageDataSnapshot.child("reactions").children.map {
            val reactionSnapshot = it.getValue<ReactionSnapshot>()!!
            reactionSnapshot.toReaction(
                uid = it.key!!,
                user = users[reactionSnapshot.authorUid],
                isSelf = userUid != null && userUid == reactionSnapshot.authorUid
            )
        }
    }

    override fun sendMessage(content: String) {
        val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid ?: return
        val messageRef = firebaseMessages.push()
        messageRef.setValue(MessageSnapshot(content, userUid))
    }

    override fun toggleReaction(emoji: String, messageUid: String) {
        val userUid = (authManager.authState.value as? AuthState.SignedIn)?.user?.uid ?: return
        val message = messages.value.first { it.uid == messageUid }
        val previousReaction = message.reactions.firstOrNull { it.isSelf && it.emoji == emoji }
        if (previousReaction != null) {
            // Reaction already exists, remove it
            firebaseMessages.child(messageUid).child("reactions").child(previousReaction.uid).removeValue()
        } else {
            // Reaction doesn't exist, add it
            val reactionRef = firebaseMessages.child(messageUid).child("reactions").push()
            reactionRef.setValue(ReactionSnapshot(emoji, userUid))
        }
    }
}