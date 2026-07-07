package com.thuo_ng.swift_chat_android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thuo_ng.swift_chat_android.data.local.dao.ConversationDao
import com.thuo_ng.swift_chat_android.data.local.dao.NotificationDao
import com.thuo_ng.swift_chat_android.data.local.dao.UserDao
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.entity.NotificationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.NotificationSummaryEntity
import com.thuo_ng.swift_chat_android.data.local.entity.UserEntity


@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        ConversationParticipantPreviewEntity::class,
        NotificationEntity::class,
        NotificationSummaryEntity::class
    ],
    exportSchema = false,
    version = 6
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun notificationDao(): NotificationDao
}
