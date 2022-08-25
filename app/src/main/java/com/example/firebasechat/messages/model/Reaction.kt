package com.example.firebasechat.messages.model

import com.example.firebasechat.auth.model.User
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

data class Reaction(
    val uid: String,
    val emoji: String,
    val createdAt: Date = Date(),
    val user: User? = null,
    val isSelf: Boolean = false
)

@IgnoreExtraProperties
data class ReactionSnapshot(
    val emoji: String? = null,
    val authorUid: String? = null,
    val createdAt: Long? = System.currentTimeMillis(), // TODO use "ServerValue.TIMESTAMP"
)

fun ReactionSnapshot.toReaction(
    uid: String,
    user: User?,
    isSelf: Boolean,
) = Reaction(
    uid = uid,
    emoji = emoji!!,
    createdAt = Date(createdAt!!),
    user = user,
    isSelf = isSelf
)