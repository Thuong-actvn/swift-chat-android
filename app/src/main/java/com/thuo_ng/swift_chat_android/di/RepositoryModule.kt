package com.thuo_ng.swift_chat_android.di

import com.thuo_ng.swift_chat_android.data.repository.AuthRepositoryImpl
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
