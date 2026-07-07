package com.thuo_ng.swift_chat_android.domain.model

enum class SendStatus {
    SENDING,
    SENT,
    FAILED
}

data class Message(
    val localId: String,
    val serverId: String?,
    val clientTempId: String?,
    val conversationId: String,
    val senderId: String,
    val sender: SenderProfile? = null,
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
    val replyTo: ReplyTarget?,
    val forwardedFrom: ForwardedFrom?,
    val reactions: List<MessageReaction>,
    val attachments: List<MessageAttachment>,
    val sendStatus: SendStatus
)

data class SenderProfile(
    val accountId: String?,
    val handle: String?,
    val displayName: String?,
    val avatarUrl: String?
)

data class MessageAttachment(
    val url: String,
    val position: Int
)

data class MessageReaction(
    val emoji: String,
    val accountId: String,
    val createdAt: String?
)

data class ReplyTarget(
    val messageId: String,
    val senderId: String,
    val content: String,
    val type: String
)

data class ForwardedFrom(
    val messageId: String,
    val conversationId: String
)

data class ReadReceipt(
    val conversationId: String,
    val accountId: String,
    val lastReadMessageId: String,
    val handle: String?,
    val displayName: String?,
    val avatarUrl: String?
)

data class TypingUser(
    val conversationId: String,
    val accountId: String,
    val displayName: String?
)
