package com.thuo_ng.swift_chat_android.di

import com.thuo_ng.swift_chat_android.core.auth.GoogleSignInHelper
import com.thuo_ng.swift_chat_android.core.auth.GoogleSignInHelperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindGoogleSignInHelper(
        googleSignInHelperImpl: GoogleSignInHelperImpl
    ): GoogleSignInHelper
}