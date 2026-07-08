package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "conversation_participant_previews",
    primaryKeys = ["conversationId", "position"],
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
        Index(value = ["accountId"]),
        Index(value = ["userId"]),
        Index(value = ["handle"])
    ]
)
data class ConversationParticipantPreviewEntity(
    val conversationId: String,
    val position: Int,
    val accountId: String?,
    val userId: String?,
    val handle: String,
    val displayName: String,
    val avatarUrl: String?
)
