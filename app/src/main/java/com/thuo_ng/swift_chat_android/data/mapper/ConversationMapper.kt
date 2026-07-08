package com.thuo_ng.swift_chat_android.data.mapper

import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.relation.ConversationWithParticipantPreviews
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationDetailDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationParticipantDto
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
        displayTitle = displayInfo.title.orEmpty(),
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
            accountId = preview.accountId,
            handle = preview.handle,
            displayName = preview.displayName?.takeIf { it.isNotBlank() } ?: preview.handle,
            avatarUrl = preview.avatarUrl
        )
    }

fun ConversationDetailDto.toEntity(
    currentAccountId: String?,
    partnerAccountId: String
): ConversationEntity {
    val partner = directPartner(currentAccountId = currentAccountId, partnerAccountId = partnerAccountId)
    val currentParticipant = participants.firstOrNull { it.accountId == currentAccountId }
    return ConversationEntity(
        id = id,
        type = type,
        displayTitle = resolvedTitle(partner),
        avatarUrl = avatarUrl ?: partner?.user?.avatarUrl,
        isOnline = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadCount = 0,
        currentParticipantRole = currentParticipant?.role,
        currentParticipantIsMuted = currentParticipant?.mutedUntil != null,
        currentParticipantMutedUntil = currentParticipant?.mutedUntil,
        currentParticipantLastReadMessageId = currentParticipant?.lastReadMessageId,
        totalParticipants = participants.size,
        lastMessageId = null,
        lastMessageContent = null,
        lastMessageSenderId = null,
        lastMessageSenderName = null,
        lastMessageTimestamp = null,
        lastMessageType = null
    )
}

fun ConversationDetailDto.toParticipantPreviewEntities(): List<ConversationParticipantPreviewEntity> =
    participants.mapIndexed { index, participant ->
        ConversationParticipantPreviewEntity(
            conversationId = id,
            position = index,
            accountId = participant.accountId,
            handle = participant.user?.handle?.takeIf { it.isNotBlank() } ?: participant.accountId,
            displayName = participant.user?.displayName?.takeIf { it.isNotBlank() }
                ?: participant.user?.handle?.takeIf { it.isNotBlank() }
                ?: participant.accountId,
            avatarUrl = participant.user?.avatarUrl
        )
    }

fun ConversationDetailDto.toDomain(
    currentAccountId: String?,
    partnerAccountId: String
): Conversation {
    val partner = directPartner(currentAccountId = currentAccountId, partnerAccountId = partnerAccountId)
    val currentParticipant = participants.firstOrNull { it.accountId == currentAccountId }
    return Conversation(
        id = id,
        type = type,
        displayInfo = DisplayInfo(
            title = resolvedTitle(partner),
            avatarUrl = avatarUrl ?: partner?.user?.avatarUrl,
            isOnline = null
        ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        unreadCount = 0,
        currentParticipant = currentParticipant?.let {
            CurrentParticipant(
                role = it.role,
                isMuted = it.mutedUntil != null,
                mutedUntil = it.mutedUntil,
                lastReadMessageId = it.lastReadMessageId
            )
        },
        participantPreview = participants.map { it.toDomain() },
        totalParticipants = participants.size,
        lastMessage = null
    )
}

private fun ConversationDetailDto.directPartner(
    currentAccountId: String?,
    partnerAccountId: String
): ConversationParticipantDto? {
    return participants.firstOrNull { it.accountId == partnerAccountId }
        ?: participants.firstOrNull { it.accountId != currentAccountId }
        ?: participants.firstOrNull()
}

private fun ConversationDetailDto.resolvedTitle(partner: ConversationParticipantDto?): String {
    return title?.takeIf { it.isNotBlank() }
        ?: partner?.user?.displayName?.takeIf { it.isNotBlank() }
        ?: partner?.user?.handle?.takeIf { it.isNotBlank() }
        ?: "Direct message"
}

private fun ConversationParticipantDto.toDomain(): ParticipantPreview =
    ParticipantPreview(
        accountId = accountId,
        handle = user?.handle?.takeIf { it.isNotBlank() } ?: accountId,
        displayName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.handle?.takeIf { it.isNotBlank() }
            ?: accountId,
        avatarUrl = user?.avatarUrl
    )

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
