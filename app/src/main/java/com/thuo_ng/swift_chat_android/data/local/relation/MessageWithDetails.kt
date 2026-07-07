package com.thuo_ng.swift_chat_android.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.thuo_ng.swift_chat_android.data.local.entity.MessageAttachmentEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageReactionEntity

data class MessageWithDetails(
    @Embedded val message: MessageEntity,
    @Relation(
        entity = MessageAttachmentEntity::class,
        parentColumn = "localId",
        entityColumn = "messageLocalId"
    )
    val attachments: List<MessageAttachmentEntity>,
    @Relation(
        entity = MessageReactionEntity::class,
        parentColumn = "localId",
        entityColumn = "messageLocalId"
    )
    val reactions: List<MessageReactionEntity>
)
