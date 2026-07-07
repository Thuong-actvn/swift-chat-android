package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_summary")
data class NotificationSummaryEntity(
    @PrimaryKey val id: String = "singleton",
    val unreadCount: Int
)
