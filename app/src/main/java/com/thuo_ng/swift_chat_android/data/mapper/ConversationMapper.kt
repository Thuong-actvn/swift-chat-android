package com.thuo_ng.swift_chat_android.data.mapper

import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.relation.ConversationWithParticipantPreviews
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationMemberDto
import com.thuo_ng.swift_chat_android.data.remote.dto.displayMessagePreviewContent
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.CurrentParticipant
import com.thuo_ng.swift_chat_android.domain.model.DisplayInfo
import com.thuo_ng.swift_chat_android.domain.model.MessagePreview
import com.thuo_ng.swift_chat_android.domain.model.ParticipantPreview

fun ConversationDto.toEntity(): ConversationEntity =
    ConversationEntity(
        id = id,
        type = type,
        displayTitle = displayInfo.title,
        avatarUrl = displayInfo.avatarUrl,
        isOnline = displayInfo.isOnline,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadCount = unreadCount,
        currentParticipantRole = currentParticipant?.role,
        currentParticipantIsMuted = currentParticipant?.isMuted ?: false,
        currentParticipantMutedUntil = currentParticipant?.mutedUntil,
        currentParticipantLastReadMessageId = currentParticipant?.lastReadMessageId,
        totalParticipants = totalParticipants,
        lastMessageId = lastMessage?.id,
        lastMessageContent = lastMessage?.let {
            displayMessagePreviewContent(it.content, it.resolvedType(), it.isUnsent, it.attachments)
        },
        lastMessageSenderId = lastMessage?.senderId.orEmpty().takeIf { lastMessage != null },
        lastMessageSenderName = lastMessage?.senderName,
        lastMessageTimestamp = lastMessage?.timestamp ?: lastMessage?.createdAt ?: updatedAt.ifBlank { createdAt },
        lastMessageType = lastMessage?.resolvedType() ?: lastMessage?.let { "text" }
    )

fun ConversationDto.toParticipantPreviewEntities(): List<ConversationParticipantPreviewEntity> =
    participantPreview.mapIndexed { index, preview ->
        ConversationParticipantPreviewEntity(
            conversationId = id,
            position = index,
            accountId = preview.accountId ?: preview.id ?: preview.userId,
            handle = preview.handle,
            displayName = preview.displayName,
            avatarUrl = preview.avatarUrl
        )
    }

fun List<ConversationMemberDto>.toParticipantPreviewEntities(
    conversationId: String
): List<ConversationParticipantPreviewEntity> =
    mapIndexed { index, member ->
        ConversationParticipantPreviewEntity(
            conversationId = conversationId,
            position = index,
            accountId = member.accountId,
            handle = member.handle?.takeIf { it.isNotBlank() } ?: member.accountId,
            displayName = member.displayName?.takeIf { it.isNotBlank() }
                ?: member.handle?.takeIf { it.isNotBlank() }
                ?: member.accountId,
            avatarUrl = member.avatarUrl
        )
    }

fun ConversationWithParticipantPreviews.toDomain(): Conversation =
    Conversation(
        id = conversation.id,
        type = conversation.type,
        displayInfo = DisplayInfo(
            title = conversation.displayTitle,
            avatarUrl = conversation.avatarUrl,
            isOnline = conversation.isOnline
        ),
        createdAt = conversation.createdAt,
        updatedAt = conversation.updatedAt,
        unreadCount = conversation.unreadCount,
        currentParticipant = conversation.currentParticipantRole?.let { role ->
            CurrentParticipant(
                role = role,
                isMuted = conversation.currentParticipantIsMuted,
                mutedUntil = conversation.currentParticipantMutedUntil,
                lastReadMessageId = conversation.currentParticipantLastReadMessageId
            )
        },
        participantPreview = participantPreviews
            .sortedBy { it.position }
            .map { it.toDomain() },
        totalParticipants = conversation.totalParticipants,
        lastMessage = conversation.lastMessageId?.let { messageId ->
            MessagePreview(
                id = messageId,
                content = displayMessagePreviewContent(
                    content = conversation.lastMessageContent,
                    type = conversation.lastMessageType,
                    isUnsent = false
                ),
                senderId = conversation.lastMessageSenderId.orEmpty(),
                senderName = conversation.lastMessageSenderName,
                timestamp = conversation.lastMessageTimestamp ?: conversation.updatedAt,
                type = conversation.lastMessageType ?: "text"
            )
        }
    )

private fun ConversationParticipantPreviewEntity.toDomain(): ParticipantPreview =
    ParticipantPreview(
        accountId = accountId,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
