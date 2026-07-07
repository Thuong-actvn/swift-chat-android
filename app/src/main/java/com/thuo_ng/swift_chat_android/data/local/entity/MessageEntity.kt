package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["createdAt"]),
        Index(value = ["serverId"], unique = true),
        Index(value = ["clientTempId"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey val localId: String,
    val serverId: String?,
    val clientTempId: String?,
    val conversationId: String,
    val senderId: String,
    val senderAccountId: String?,
    val senderHandle: String?,
    val senderDisplayName: String?,
    val senderAvatarUrl: String?,
    val content: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String?,
    val isUnsent: Boolean,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val isPinned: Boolean,
    val pinnedBy: String?,
    val pinnedAt: String?,
    val replyToMessageId: String?,
    val replyToSenderId: String?,
    val replyToContent: String?,
    val replyToType: String?,
    val forwardedFromMessageId: String?,
    val forwardedFromConversationId: String?,
    val sendStatus: String
)
