package com.thuo_ng.swift_chat_android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thuo_ng.swift_chat_android.data.local.dao.ConversationDao
import com.thuo_ng.swift_chat_android.data.local.dao.UserDao
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.entity.UserEntity


@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        ConversationParticipantPreviewEntity::class
    ],
    exportSchema = false,
    version = 4
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
}
