package com.thuo_ng.swift_chat_android.di

import android.content.Context
import androidx.room.Room
import com.thuo_ng.swift_chat_android.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ) : AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "swift_chat_db"
    )
        .fallbackToDestructiveMigration(true)
        .build()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase) = db.userDao()

    @Provides
    @Singleton
    fun provideConversationDao(db: AppDatabase) = db.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase) = db.messageDao()

    @Provides
    @Singleton
    fun provideReadReceiptDao(db: AppDatabase) = db.readReceiptDao()

    @Provides
    @Singleton
    fun provideNotificationDao(db: AppDatabase) = db.notificationDao()
}
