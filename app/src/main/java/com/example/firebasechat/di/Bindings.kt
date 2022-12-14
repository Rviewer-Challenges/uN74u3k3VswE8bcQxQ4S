package com.example.firebasechat.di

import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.auth.AuthManagerImpl
import com.example.firebasechat.messages.MessageRepo
import com.example.firebasechat.messages.MessagesRepoImpl
import com.example.firebasechat.settings.SettingsStore
import com.example.firebasechat.settings.SettingsStoreImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class Bindings {

    @Binds
    abstract fun bindAuthManager(
        authManagerImpl: AuthManagerImpl
    ): AuthManager

    @Binds
    abstract fun bindMessageRepo(
        messagesRepoImpl: MessagesRepoImpl
    ): MessageRepo

    @Binds
    abstract fun bindSettingsStore(
        settingsStoreImpl: SettingsStoreImpl
    ): SettingsStore
}