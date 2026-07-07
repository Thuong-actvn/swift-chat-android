package com.thuo_ng.swift_chat_android.di

import com.thuo_ng.swift_chat_android.data.repository.AuthRepositoryImpl
import com.thuo_ng.swift_chat_android.data.repository.ConversationRepositoryImpl
import com.thuo_ng.swift_chat_android.data.repository.UserRepositoryImpl
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        conversationRepositoryImpl: ConversationRepositoryImpl
    ): ConversationRepository
}
