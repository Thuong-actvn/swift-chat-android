package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["type"]),
        Index(value = ["updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val displayTitle: String,
    val avatarUrl: String?,
    val isOnline: Boolean?,
    val createdAt: String,
    val updatedAt: String,
    val unreadCount: Int,
    val currentParticipantRole: String?,
    val currentParticipantIsMuted: Boolean,
    val currentParticipantMutedUntil: String?,
    val currentParticipantLastReadMessageId: String?,
    val totalParticipants: Int,
    val lastMessageId: String?,
    val lastMessageContent: String?,
    val lastMessageSenderId: String?,
    val lastMessageSenderName: String?,
    val lastMessageTimestamp: String?,
    val lastMessageType: String?
)
