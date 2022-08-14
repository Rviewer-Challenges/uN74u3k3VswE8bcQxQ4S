package com.example.firebasechat.di

import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.auth.AuthManagerImpl
import com.example.firebasechat.data.MessageRepo
import com.example.firebasechat.data.MessagesRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class Modules {

    @Binds
    abstract fun bindAuthManager(
        authManagerImpl: AuthManagerImpl
    ): AuthManager

    @Binds
    abstract fun bindMessageRepo(
        messagesRepoImpl: MessagesRepoImpl
    ): MessageRepo
}