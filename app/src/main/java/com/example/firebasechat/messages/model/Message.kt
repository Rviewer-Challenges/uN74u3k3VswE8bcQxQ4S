package com.example.firebasechat.messages.model

import com.example.firebasechat.auth.model.User
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

data class Message(
    val uid: String,
    val text: String,
    val createdAt: Date = Date(),
    val reactions: List<Reaction> = emptyList(),
    val user: User? = null,
    val isSelf: Boolean = false
)

@IgnoreExtraProperties
data class MessageSnapshot(
    val text: String? = null,
    val authorUid: String? = null,
    val createdAt: Long? = System.currentTimeMillis(), // TODO use "ServerValue.TIMESTAMP"
)

fun MessageSnapshot.toMessage(
    uid: String,
    user: User?,
    isSelf: Boolean,
    reactions: List<Reaction>
) = Message(
    uid = uid,
    text = text!!,
    createdAt = Date(createdAt!!),
    reactions = reactions,
    user = user,
    isSelf = isSelf
)