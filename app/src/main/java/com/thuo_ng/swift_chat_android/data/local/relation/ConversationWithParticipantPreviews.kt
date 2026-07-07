package com.thuo_ng.swift_chat_android.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity

data class ConversationWithParticipantPreviews(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        entity = ConversationParticipantPreviewEntity::class,
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val participantPreviews: List<ConversationParticipantPreviewEntity>
)
