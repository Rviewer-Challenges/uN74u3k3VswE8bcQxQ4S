package com.example.firebasechat.data.models

import com.google.firebase.database.IgnoreExtraProperties

data class Message(
    val id: String,
    val text: String,
    val authorUid: String,
    val isSelf: Boolean
)

@IgnoreExtraProperties
data class MessageSnapshot(
    val id: String? = null,
    val text: String? = null,
    val authorUid: String? = null,
)

fun MessageSnapshot.toMessage(userUid: String?) = Message(
    id = id!!,
    text = text!!,
    authorUid = authorUid!!,
    isSelf = userUid != null && userUid == authorUid
)