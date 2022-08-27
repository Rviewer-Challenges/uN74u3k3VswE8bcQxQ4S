package com.example.firebasechat.messages.model

import androidx.compose.runtime.Immutable
import com.example.firebasechat.auth.model.User
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
data class Message(
    val uid: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reactions: ImmutableList<Reaction> = persistentListOf(),
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
    createdAt = createdAt!!,
    reactions = reactions.toPersistentList(),
    user = user,
    isSelf = isSelf
)