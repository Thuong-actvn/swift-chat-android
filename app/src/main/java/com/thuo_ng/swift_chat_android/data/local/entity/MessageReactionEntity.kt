package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["messageLocalId", "emoji", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["localId"],
            childColumns = ["messageLocalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageLocalId"]),
        Index(value = ["accountId"])
    ]
)
data class MessageReactionEntity(
    val messageLocalId: String,
    val emoji: String,
    val accountId: String,
    val createdAt: String?
)
