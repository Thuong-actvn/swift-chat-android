package com.thuo_ng.swift_chat_android.data.mapper

import com.thuo_ng.swift_chat_android.core.socket.ForwardedFromPayload
import com.thuo_ng.swift_chat_android.core.socket.MessagePayload
import com.thuo_ng.swift_chat_android.core.socket.ReactionPayload
import com.thuo_ng.swift_chat_android.core.socket.ReplyTargetPayload
import com.thuo_ng.swift_chat_android.core.socket.SenderPayload
import com.thuo_ng.swift_chat_android.data.local.entity.MessageAttachmentEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageReactionEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ReadReceiptEntity
import com.thuo_ng.swift_chat_android.data.local.relation.MessageWithDetails
import com.thuo_ng.swift_chat_android.data.remote.dto.ForwardedFromDto
import com.thuo_ng.swift_chat_android.data.remote.dto.MessageDto
import com.thuo_ng.swift_chat_android.data.remote.dto.MessageSenderDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ReactionDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ReplyTargetDto
import com.thuo_ng.swift_chat_android.domain.model.ForwardedFrom
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.MessageAttachment
import com.thuo_ng.swift_chat_android.domain.model.MessageReaction
import com.thuo_ng.swift_chat_android.domain.model.ReadReceipt
import com.thuo_ng.swift_chat_android.domain.model.ReplyTarget
import com.thuo_ng.swift_chat_android.domain.model.SendStatus
import com.thuo_ng.swift_chat_android.domain.model.SenderProfile
import java.time.Instant

data class MessageEntityGraph(
    val message: MessageEntity,
    val attachments: List<MessageAttachmentEntity>,
    val reactions: List<MessageReactionEntity>
)

fun MessageDto.toEntityGraph(
    localIdOverride: String? = null,
    sendStatus: SendStatus = SendStatus.SENT
): MessageEntityGraph {
    val localId = localIdOverride ?: clientTempId ?: id
    val senderProfile = toSenderProfile()
    return MessageEntityGraph(
        message = MessageEntity(
            localId = localId,
            serverId = id,
            clientTempId = clientTempId,
            conversationId = conversationId,
            senderId = senderId,
            senderAccountId = senderProfile?.accountId,
            senderHandle = senderProfile?.handle,
            senderDisplayName = senderProfile?.displayName,
            senderAvatarUrl = senderProfile?.avatarUrl,
            content = content.orEmpty(),
            type = type,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isUnsent = isUnsent,
            isEdited = isEdited,
            isDeleted = isDeleted,
            isPinned = isPinned == true,
            pinnedBy = pinnedBy,
            pinnedAt = pinnedAt,
            replyToMessageId = replyTo?.messageId,
            replyToSenderId = replyTo?.senderId,
            replyToContent = replyTo?.content,
            replyToType = replyTo?.type,
            forwardedFromMessageId = forwardedFrom?.messageId,
            forwardedFromConversationId = forwardedFrom?.conversationId,
            sendStatus = sendStatus.name
        ),
        attachments = attachments.orEmpty().mapIndexed { index, url ->
            MessageAttachmentEntity(localId, index, url)
        },
        reactions = reactions.toDtoReactionEntities(localId)
    )
}

fun MessagePayload.toEntityGraph(sendStatus: SendStatus = SendStatus.SENT): MessageEntityGraph {
    val localId = clientTempId ?: id
    val senderProfile = toSenderProfile()
    return MessageEntityGraph(
        message = MessageEntity(
            localId = localId,
            serverId = id,
            clientTempId = clientTempId,
            conversationId = conversationId,
            senderId = senderId,
            senderAccountId = senderProfile?.accountId,
            senderHandle = senderProfile?.handle,
            senderDisplayName = senderProfile?.displayName,
            senderAvatarUrl = senderProfile?.avatarUrl,
            content = content.orEmpty(),
            type = type,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isUnsent = isUnsent,
            isEdited = isEdited,
            isDeleted = isDeleted,
            isPinned = isPinned == true,
            pinnedBy = pinnedBy,
            pinnedAt = pinnedAt,
            replyToMessageId = replyTo?.messageId,
            replyToSenderId = replyTo?.senderId,
            replyToContent = replyTo?.content,
            replyToType = replyTo?.type,
            forwardedFromMessageId = forwardedFrom?.messageId,
            forwardedFromConversationId = forwardedFrom?.conversationId,
            sendStatus = sendStatus.name
        ),
        attachments = attachments.orEmpty().mapIndexed { index, url ->
            MessageAttachmentEntity(localId, index, url)
        },
        reactions = reactions.toPayloadReactionEntities(localId)
    )
}

fun optimisticMessageGraph(
    conversationId: String,
    senderId: String,
    content: String,
    type: String,
    clientTempId: String,
    attachments: List<String> = emptyList()
): MessageEntityGraph {
    val now = Instant.now().toString()
    return MessageEntityGraph(
        message = MessageEntity(
            localId = clientTempId,
            serverId = null,
            clientTempId = clientTempId,
            conversationId = conversationId,
            senderId = senderId,
            senderAccountId = null,
            senderHandle = null,
            senderDisplayName = null,
            senderAvatarUrl = null,
            content = content,
            type = type,
            createdAt = now,
            updatedAt = null,
            isUnsent = false,
            isEdited = false,
            isDeleted = false,
            isPinned = false,
            pinnedBy = null,
            pinnedAt = null,
            replyToMessageId = null,
            replyToSenderId = null,
            replyToContent = null,
            replyToType = null,
            forwardedFromMessageId = null,
            forwardedFromConversationId = null,
            sendStatus = SendStatus.SENDING.name
        ),
        attachments = attachments.mapIndexed { index, url ->
            MessageAttachmentEntity(clientTempId, index, url)
        },
        reactions = emptyList()
    )
}

fun MessageWithDetails.toDomain(): Message {
    val entity = message
    return Message(
        localId = entity.localId,
        serverId = entity.serverId,
        clientTempId = entity.clientTempId,
        conversationId = entity.conversationId,
        senderId = entity.senderId,
        sender = entity.toSenderProfile(),
        content = entity.content,
        type = entity.type,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        isUnsent = entity.isUnsent,
        isEdited = entity.isEdited,
        isDeleted = entity.isDeleted,
        isPinned = entity.isPinned,
        pinnedBy = entity.pinnedBy,
        pinnedAt = entity.pinnedAt,
        replyTo = entity.replyToMessageId?.let {
            ReplyTarget(
                messageId = it,
                senderId = entity.replyToSenderId.orEmpty(),
                content = entity.replyToContent.orEmpty(),
                type = entity.replyToType ?: "text"
            )
        },
        forwardedFrom = entity.forwardedFromMessageId?.let {
            ForwardedFrom(
                messageId = it,
                conversationId = entity.forwardedFromConversationId.orEmpty()
            )
        },
        reactions = reactions.map { it.toDomain() },
        attachments = attachments.sortedBy { it.position }.map { it.toDomain() },
        sendStatus = runCatching { SendStatus.valueOf(entity.sendStatus) }.getOrDefault(SendStatus.SENT)
    )
}

fun ReadReceiptEntity.toDomain(): ReadReceipt =
    ReadReceipt(
        conversationId = conversationId,
        accountId = accountId,
        lastReadMessageId = lastReadMessageId,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )

fun ReplyTargetDto.toDomain(): ReplyTarget =
    ReplyTarget(messageId = messageId, senderId = senderId, content = content, type = type)

fun ForwardedFromDto.toDomain(): ForwardedFrom =
    ForwardedFrom(messageId = messageId, conversationId = conversationId)

fun ReactionDto.toDomain(): MessageReaction =
    MessageReaction(emoji = emoji, accountId = accountId.orEmpty(), createdAt = createdAt)

private fun MessageAttachmentEntity.toDomain(): MessageAttachment =
    MessageAttachment(url = url, position = position)

private fun MessageEntity.toSenderProfile(): SenderProfile? {
    if (
        senderAccountId.isNullOrBlank() &&
        senderHandle.isNullOrBlank() &&
        senderDisplayName.isNullOrBlank() &&
        senderAvatarUrl.isNullOrBlank()
    ) {
        return null
    }
    return SenderProfile(
        accountId = senderAccountId,
        handle = senderHandle,
        displayName = senderDisplayName,
        avatarUrl = senderAvatarUrl
    )
}

private fun MessageDto.toSenderProfile(): SenderProfile? {
    val accountId = sender?.accountId
        ?: sender?.id
        ?: sender?.userId
        ?: senderId.takeIf { it.isNotBlank() }
    val handle = sender?.handle
        ?: sender?.username
        ?: senderHandle
        ?: handle
    val displayName = sender?.displayName
        ?: sender?.name
        ?: senderDisplayName
        ?: senderName
        ?: displayName
    val avatarUrl = sender?.avatarUrl
        ?: sender?.avatar
        ?: senderAvatarUrl
        ?: senderAvatar
        ?: avatarUrl

    return buildSenderProfile(
        accountId = accountId,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
}

private fun MessagePayload.toSenderProfile(): SenderProfile? {
    val accountId = sender?.accountId
        ?: sender?.id
        ?: sender?.userId
        ?: senderId.takeIf { it.isNotBlank() }
    val handle = sender?.handle
        ?: sender?.username
        ?: senderHandle
        ?: handle
    val displayName = sender?.displayName
        ?: sender?.name
        ?: senderDisplayName
        ?: senderName
        ?: displayName
    val avatarUrl = sender?.avatarUrl
        ?: sender?.avatar
        ?: senderAvatarUrl
        ?: senderAvatar
        ?: avatarUrl

    return buildSenderProfile(
        accountId = accountId,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
}

private fun buildSenderProfile(
    accountId: String?,
    handle: String?,
    displayName: String?,
    avatarUrl: String?
): SenderProfile? {
    val hasProfileInfo = !handle.isNullOrBlank() ||
        !displayName.isNullOrBlank() ||
        !avatarUrl.isNullOrBlank()
    if (!hasProfileInfo) return null

    return SenderProfile(
        accountId = accountId,
        handle = handle?.takeIf { it.isNotBlank() },
        displayName = displayName?.takeIf { it.isNotBlank() },
        avatarUrl = avatarUrl?.takeIf { it.isNotBlank() }
    )
}

private fun MessageReactionEntity.toDomain(): MessageReaction =
    MessageReaction(emoji = emoji, accountId = accountId, createdAt = createdAt)

private fun List<ReactionPayload>?.toPayloadReactionEntities(localId: String): List<MessageReactionEntity> {
    return orEmpty().flatMap { reaction ->
        if (reaction.emoji.isBlank()) return@flatMap emptyList()
        reaction.accountIds().map { accountId ->
            MessageReactionEntity(
                messageLocalId = localId,
                emoji = reaction.emoji,
                accountId = accountId,
                createdAt = reaction.createdAt
            )
        }
    }
}

private fun List<ReactionDto>?.toDtoReactionEntities(localId: String): List<MessageReactionEntity> {
    return orEmpty().flatMap { reaction ->
        if (reaction.emoji.isBlank()) return@flatMap emptyList()
        reaction.accountIds().map { accountId ->
            MessageReactionEntity(
                messageLocalId = localId,
                emoji = reaction.emoji,
                accountId = accountId,
                createdAt = reaction.createdAt
            )
        }
    }
}

private fun ReactionPayload.accountIds(): List<String> =
    (accountId?.let(::listOf) ?: userIds.orEmpty())
        .filter { it.isNotBlank() }

private fun ReactionDto.accountIds(): List<String> =
    (accountId?.let(::listOf) ?: userIds.orEmpty())
        .filter { it.isNotBlank() }

fun ReplyTargetPayload.toDomain(): ReplyTarget =
    ReplyTarget(messageId = messageId, senderId = senderId, content = content, type = type)

fun ForwardedFromPayload.toDomain(): ForwardedFrom =
    ForwardedFrom(messageId = messageId, conversationId = conversationId)
