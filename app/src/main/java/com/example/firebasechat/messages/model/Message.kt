package com.example.firebasechat.messages.model

import com.example.firebasechat.auth.model.User
import com.google.firebase.database.IgnoreExtraProperties

data class Message(
    val uid: String,
    val text: String,
    val user: User?,
    val isSelf: Boolean
)

@IgnoreExtraProperties
data class MessageSnapshot(
    val text: String? = null,
    val authorUid: String? = null,
)

fun MessageSnapshot.toMessage(uid: String, user: User?, isSelf: Boolean) = Message(
    uid = uid,
    text = text!!,
    user = user,
    isSelf = isSelf
)