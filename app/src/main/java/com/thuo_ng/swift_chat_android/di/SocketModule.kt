package com.thuo_ng.swift_chat_android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {
    // SocketManager được annotate @Singleton và @Inject constructor
    // nên Hilt có thể tự động provide. Module này để trống làm
    // placeholder cho các dependency liên quan socket sau này.
}
