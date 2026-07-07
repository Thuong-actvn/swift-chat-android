package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_attachments",
    primaryKeys = ["messageLocalId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["localId"],
            childColumns = ["messageLocalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageLocalId"])]
)
data class MessageAttachmentEntity(
    val messageLocalId: String,
    val position: Int,
    val url: String
)
