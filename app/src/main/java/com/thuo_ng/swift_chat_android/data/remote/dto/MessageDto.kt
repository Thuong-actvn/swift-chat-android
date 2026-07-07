package com.thuo_ng.swift_chat_android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("id") val id: String,
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("sender") val sender: MessageSenderDto? = null,
    @SerializedName("senderHandle") val senderHandle: String? = null,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("senderDisplayName") val senderDisplayName: String? = null,
    @SerializedName("senderAvatar") val senderAvatar: String? = null,
    @SerializedName("senderAvatarUrl") val senderAvatarUrl: String? = null,
    @SerializedName("handle") val handle: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("content") val content: String?,
    @SerializedName("type") val type: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("isUnsent") val isUnsent: Boolean = false,
    @SerializedName("isEdited") val isEdited: Boolean = false,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("isPinned") val isPinned: Boolean? = false,
    @SerializedName("pinnedBy") val pinnedBy: String? = null,
    @SerializedName("pinnedAt") val pinnedAt: String? = null,
    @SerializedName("replyTo") val replyTo: ReplyTargetDto? = null,
    @SerializedName("forwardedFrom") val forwardedFrom: ForwardedFromDto? = null,
    @SerializedName("reactions") val reactions: List<ReactionDto>? = emptyList(),
    @SerializedName("attachments") val attachments: List<String>? = emptyList(),
    @SerializedName("clientTempId") val clientTempId: String? = null
)

data class MessageSenderDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("accountId") val accountId: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("handle") val handle: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class ReplyTargetDto(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: String
)

data class ForwardedFromDto(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("conversationId") val conversationId: String
)

data class ReactionDto(
    @SerializedName("emoji") val emoji: String = "",
    @SerializedName("accountId") val accountId: String? = null,
    @SerializedName("userIds") val userIds: List<String>? = emptyList(),
    @SerializedName("count") val count: Int = 0,
    @SerializedName("createdAt") val createdAt: String? = null
)
