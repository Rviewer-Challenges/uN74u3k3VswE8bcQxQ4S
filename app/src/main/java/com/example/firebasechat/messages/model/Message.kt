package com.example.firebasechat.messages.model

import com.example.firebasechat.auth.model.User
import com.google.firebase.database.IgnoreExtraProperties
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.*

data class Message(
    val uid: String,
    val text: String,
    val createdAt: Date = Date(),
    val reactions: List<String> = emptyList(),
    val user: User? = null,
    val isSelf: Boolean = false
)

@IgnoreExtraProperties
data class MessageSnapshot(
    val text: String? = null,
    val authorUid: String? = null,
    val createdAt: Long? = System.currentTimeMillis(), // TODO use "ServerValue.TIMESTAMP"
    val reactionsJson: String? = "[]"
)

private val adapter = Moshi.Builder().build().adapter<List<String>>(
    Types.newParameterizedType(List::class.java, String::class.java)
)

fun MessageSnapshot.toMessage(uid: String, user: User?, isSelf: Boolean) = Message(
    uid = uid,
    text = text!!,
    createdAt = Date(createdAt!!),
    reactions = adapter.fromJson(reactionsJson!!)!!,
    user = user,
    isSelf = isSelf
)