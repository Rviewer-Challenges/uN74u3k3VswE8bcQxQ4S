package com.example.firebasechat.messages.model

import com.example.firebasechat.auth.model.User
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

data class Message(
    val uid: String,
    val text: String,
    val createdAt: Date,
    val user: User?,
    val isSelf: Boolean
)

@IgnoreExtraProperties
data class MessageSnapshot(
    val text: String? = null,
    val authorUid: String? = null,
    val createdAt: Long? = System.currentTimeMillis(), // TODO use "ServerValue.TIMESTAMP"
)

fun MessageSnapshot.toMessage(uid: String, user: User?, isSelf: Boolean) = Message(
    uid = uid,
    text = text!!,
    createdAt = Date(createdAt!!),
    user = user,
    isSelf = isSelf
)