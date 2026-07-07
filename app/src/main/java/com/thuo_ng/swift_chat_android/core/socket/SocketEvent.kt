package com.thuo_ng.swift_chat_android.core.socket

sealed class SocketEvent {

    // ── Chat events ──────────────────────────────────────────────
    data class ReceiveMessage(val payload: MessagePayload) : SocketEvent()
    data class UserTyping(val conversationId: String, val userId: String, val username: String) : SocketEvent()
    data class UserStopTyping(val conversationId: String, val userId: String) : SocketEvent()
    data class MessageUnsent(val messageId: String, val conversationId: String) : SocketEvent()
    data class MessageDeletedForMe(val messageId: String) : SocketEvent()
    data class MessageEdited(val messageId: String, val newContent: String, val conversationId: String) : SocketEvent()
    data class ReadReceipt(val conversationId: String, val userId: String, val lastReadMessageId: String) : SocketEvent()
    data class ReactionUpdated(val messageId: String, val reactions: List<ReactionPayload>) : SocketEvent()
    data class MessagePinned(val messageId: String, val conversationId: String) : SocketEvent()
    data class MessageUnpinned(val messageId: String, val conversationId: String) : SocketEvent()

    // ── Presence ─────────────────────────────────────────────────
    data class PresenceStatus(val userId: String, val accountId: String, val status: String, val timestamp: Long) : SocketEvent()

    // ── Notification ─────────────────────────────────────────────
    data class NewNotification(val id: String, val type: String, val referenceId: String?, val isRead: Boolean, val createdAt: String, val actor: ActorPayload?) : SocketEvent()

    // ── Friend ───────────────────────────────────────────────────
    object FriendUpdated : SocketEvent()   // signal → client tự refetch

    // ── Group ────────────────────────────────────────────────────
    data class GroupInfoUpdated(val conversationId: String, val title: String?, val avatarUrl: String?, val updatedBy: String) : SocketEvent()
    data class GroupMemberAdded(val conversationId: String, val addedUserIds: List<String>, val addedBy: String) : SocketEvent()
    data class GroupMemberRemoved(val conversationId: String, val removedUserId: String, val removedBy: String) : SocketEvent()
    data class GroupDisbanded(val conversationId: String, val disbandedBy: String) : SocketEvent()
    data class GroupRoleChanged(val conversationId: String, val targetUserId: String, val newRole: String, val changedBy: String) : SocketEvent()
    data class GroupYouAdded(val conversationId: String, val addedBy: String) : SocketEvent()
}

// ── Payload data classes ──────────────────────────────────────
data class MessagePayload(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: String,             // "text" | "image" | "video" | "file"
    val clientTempId: String?,    // Optimistic update matching
    val replyToId: String?,
    val isDeleted: Boolean,
    val isEdited: Boolean,
    val createdAt: String,
    val reactions: List<ReactionPayload> = emptyList()
)

data class ReactionPayload(val emoji: String, val count: Int, val userIds: List<String>)
data class ActorPayload(val id: String, val username: String, val avatarUrl: String?)
