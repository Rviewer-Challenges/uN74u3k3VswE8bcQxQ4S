package com.example.firebasechat.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Modules {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class DispatcherDefault

    @Provides
    @Singleton
    fun provideExternalScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    @DispatcherDefault
    fun provideDispatcherDefault(): CoroutineDispatcher = Dispatchers.Default
}