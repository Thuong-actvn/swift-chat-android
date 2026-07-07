package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "read_receipts",
    primaryKeys = ["conversationId", "accountId"],
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
        Index(value = ["accountId"])
    ]
)
data class ReadReceiptEntity(
    val conversationId: String,
    val accountId: String,
    val lastReadMessageId: String,
    val handle: String?,
    val displayName: String?,
    val avatarUrl: String?
)
