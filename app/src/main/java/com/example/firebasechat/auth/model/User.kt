package com.example.firebasechat.auth.model

import com.google.firebase.database.IgnoreExtraProperties

data class User (
    val uid: String,
    val name: String?,
    val photoUrl: String?
)

@IgnoreExtraProperties
data class UserSnapshot(
    val name: String? = null,
    val photoUrl: String? = null
)

fun UserSnapshot.toUser(uid: String) = User(
    uid = uid,
    name = name,
    photoUrl = photoUrl
)