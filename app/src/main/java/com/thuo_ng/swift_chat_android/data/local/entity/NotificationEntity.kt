package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["type"]),
        Index(value = ["isRead"]),
        Index(value = ["createdAt"])
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val referenceId: String?,
    val isRead: Boolean,
    val createdAt: String,
    val actorId: String?,
    val actorHandle: String?,
    val actorDisplayName: String?,
    val actorAvatarUrl: String?,
    val title: String,
    val message: String,
    val payloadJson: String?
)