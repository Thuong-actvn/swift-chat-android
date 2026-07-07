package com.thuo_ng.swift_chat_android.domain.model

data class Conversation(
    val id: String,
    val type: String, // "direct" or "group"
    val displayInfo: DisplayInfo,
    val createdAt: String,
    val updatedAt: String,
    val unreadCount: Int,
    val currentParticipant: CurrentParticipant?,
    val participantPreview: List<ParticipantPreview>,
    val totalParticipants: Int,
    val lastMessage: MessagePreview?
)

data class ConversationPage(
    val conversations: List<Conversation>,
    val nextCursor: String?,
    val hasMore: Boolean
)

data class DisplayInfo(
    val title: String,
    val avatarUrl: String?,
    val isOnline: Boolean?
)

data class CurrentParticipant(
    val role: String,
    val isMuted: Boolean,
    val mutedUntil: String?,
    val lastReadMessageId: String?
)

data class ParticipantPreview(
    val accountId: String? = null,
    val handle: String,
    val displayName: String,
    val avatarUrl: String?
)

data class MessagePreview(
    val id: String,
    val content: String,
    val senderId: String,
    val senderName: String?,
    val timestamp: String,
    val type: String // "text", "image", etc.
)
