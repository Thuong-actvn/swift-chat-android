package com.thuo_ng.swift_chat_android.core.socket

sealed class SocketEvent {
    data class ReceiveMessage(val payload: MessagePayload) : SocketEvent()
    data class UserTyping(
        val conversationId: String,
        val accountId: String,
        val displayName: String? = null,
        val timestamp: String? = null
    ) : SocketEvent()
    data class UserStopTyping(
        val conversationId: String,
        val accountId: String,
        val timestamp: String? = null
    ) : SocketEvent()
    data class MessageUnsent(
        val messageId: String,
        val conversationId: String,
        val timestamp: String? = null
    ) : SocketEvent()
    data class MessageDeletedForMe(
        val messageId: String,
        val conversationId: String,
        val timestamp: String? = null
    ) : SocketEvent()
    data class MessageEdited(
        val messageId: String,
        val conversationId: String,
        val content: String,
        val editedBy: String? = null,
        val timestamp: String? = null
    ) : SocketEvent()
    data class ReadReceipt(
        val conversationId: String,
        val accountId: String? = null,
        val userId: String? = null,
        val messageId: String,
        val handle: String? = null,
        val displayName: String? = null,
        val avatarUrl: String? = null,
        val timestamp: String? = null
    ) : SocketEvent()
    data class ReactionUpdated(
        val conversationId: String,
        val messageId: String,
        val reactions: List<ReactionPayload>? = emptyList()
    ) : SocketEvent()
    data class MessagePinned(
        val messageId: String,
        val conversationId: String,
        val pinnedBy: String? = null,
        val timestamp: String? = null
    ) : SocketEvent()
    data class MessageUnpinned(
        val messageId: String,
        val conversationId: String,
        val timestamp: String? = null
    ) : SocketEvent()

    data class PresenceStatus(
        val conversationId: String? = null,
        val userId: String? = null,
        val accountId: String? = null,
        val status: String? = null,
        val isOnline: Boolean? = null,
        val lastSeen: String? = null,
        val timestamp: Long? = null
    ) : SocketEvent()

    data class NewNotification(
        val id: String,
        val type: String,
        val referenceId: String?,
        val conversationId: String? = null,
        val payload: NotificationDataPayload? = null,
        val message: MessagePayload? = null,
        val isRead: Boolean,
        val createdAt: String,
        val actor: ActorPayload?
    ) : SocketEvent()

    data object FriendUpdated : SocketEvent()

    data class GroupInfoUpdated(
        val conversationId: String,
        val title: String?,
        val avatarUrl: String?,
        val updatedBy: String
    ) : SocketEvent()
    data class GroupMemberAdded(val conversationId: String, val addedUserIds: List<String>, val addedBy: String) : SocketEvent()
    data class GroupMemberRemoved(val conversationId: String, val removedUserId: String, val removedBy: String) : SocketEvent()
    data class GroupDisbanded(val conversationId: String, val disbandedBy: String) : SocketEvent()
    data class GroupRoleChanged(val conversationId: String, val targetUserId: String, val newRole: String, val changedBy: String) : SocketEvent()
    data class GroupYouAdded(val conversationId: String, val addedBy: String) : SocketEvent()
}

data class MessagePayload(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val sender: SenderPayload? = null,
    val senderHandle: String? = null,
    val senderName: String? = null,
    val senderDisplayName: String? = null,
    val senderAvatar: String? = null,
    val senderAvatarUrl: String? = null,
    val handle: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val content: String? = "",
    val type: String,
    val clientTempId: String? = null,
    val replyToMessageId: String? = null,
    val replyTo: ReplyTargetPayload? = null,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val isUnsent: Boolean = false,
    val isPinned: Boolean? = false,
    val pinnedBy: String? = null,
    val pinnedAt: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val attachments: List<String>? = emptyList(),
    val reactions: List<ReactionPayload>? = emptyList(),
    val forwardedFrom: ForwardedFromPayload? = null
)

data class SenderPayload(
    val id: String? = null,
    val accountId: String? = null,
    val userId: String? = null,
    val handle: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val name: String? = null,
    val avatar: String? = null,
    val avatarUrl: String? = null
)

data class ReplyTargetPayload(
    val messageId: String,
    val senderId: String,
    val content: String,
    val type: String
)

data class ForwardedFromPayload(
    val messageId: String,
    val conversationId: String
)

data class NotificationDataPayload(
    val conversationId: String? = null,
    val messageId: String? = null,
    val referenceId: String? = null
)

data class ReactionPayload(
    val emoji: String = "",
    val count: Int = 0,
    val userIds: List<String>? = emptyList(),
    val accountId: String? = null,
    val createdAt: String? = null
)

data class ActorPayload(
    val id: String? = null,
    val username: String? = null,
    val handle: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null
)
