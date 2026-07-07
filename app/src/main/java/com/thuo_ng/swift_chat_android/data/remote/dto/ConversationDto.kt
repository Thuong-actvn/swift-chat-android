package com.thuo_ng.swift_chat_android.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.ConversationPage
import com.thuo_ng.swift_chat_android.domain.model.CurrentParticipant
import com.thuo_ng.swift_chat_android.domain.model.DisplayInfo
import com.thuo_ng.swift_chat_android.domain.model.MessagePreview
import com.thuo_ng.swift_chat_android.domain.model.ParticipantPreview

data class ConversationResponseDto(
    @SerializedName("data") val data: List<ConversationDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
    @SerializedName("hasMore") val hasMore: Boolean
) {
    fun toDomain(): ConversationPage = ConversationPage(
        conversations = data.map { it.toDomain() },
        nextCursor = nextCursor,
        hasMore = hasMore
    )
}

data class ConversationDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("displayInfo") val displayInfo: DisplayInfoDto,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("unreadCount") val unreadCount: Int,
    @SerializedName("currentParticipant") val currentParticipant: CurrentParticipantDto?,
    @SerializedName("participantPreview") val participantPreview: List<ParticipantPreviewDto>,
    @SerializedName("totalParticipants") val totalParticipants: Int,
    @SerializedName("lastMessage") val lastMessage: MessagePreviewDto?
) {
    fun toDomain(): Conversation {
        return Conversation(
            id = id,
            type = type,
            displayInfo = displayInfo.toDomain(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            unreadCount = unreadCount,
            currentParticipant = currentParticipant?.toDomain(),
            participantPreview = participantPreview.map { it.toDomain() },
            totalParticipants = totalParticipants,
            lastMessage = lastMessage?.toDomain(fallbackTimestamp = updatedAt.ifBlank { createdAt })
        )
    }
}

data class DisplayInfoDto(
    @SerializedName("title") val title: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("isOnline") val isOnline: Boolean?
) {
    fun toDomain(): DisplayInfo = DisplayInfo(
        title = title,
        avatarUrl = avatarUrl,
        isOnline = isOnline
    )
}

data class CurrentParticipantDto(
    @SerializedName("role") val role: String,
    @SerializedName("isMuted") val isMuted: Boolean,
    @SerializedName("mutedUntil") val mutedUntil: String?,
    @SerializedName("lastReadMessageId") val lastReadMessageId: String?
) {
    fun toDomain(): CurrentParticipant = CurrentParticipant(
        role = role,
        isMuted = isMuted,
        mutedUntil = mutedUntil,
        lastReadMessageId = lastReadMessageId
    )
}

data class ParticipantPreviewDto(
    @SerializedName("accountId") val accountId: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("handle") val handle: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?
) {
    fun toDomain(): ParticipantPreview = ParticipantPreview(
        accountId = accountId ?: id ?: userId,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
}

data class ConversationMemberDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("accountId") val accountId: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("joinAt") val joinAt: String? = null,
    @SerializedName("joinedAt") val joinedAt: String? = null,
    @SerializedName("handle") val handle: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?
)

data class MessagePreviewDto(
    @SerializedName("id") val id: String,
    @SerializedName("content") val content: String?,
    @SerializedName("senderId") val senderId: String?,
    @SerializedName("senderName") val senderName: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("messageType") val messageType: String? = null,
    @SerializedName("contentType") val contentType: String? = null,
    @SerializedName("mediaType") val mediaType: String? = null,
    @SerializedName("attachments") val attachments: List<String>? = emptyList(),
    @SerializedName("isUnsent") val isUnsent: Boolean = false
) {
    fun toDomain(fallbackTimestamp: String): MessagePreview = MessagePreview(
        id = id,
        content = displayMessagePreviewContent(content, resolvedType(), isUnsent, attachments),
        senderId = senderId.orEmpty(),
        senderName = senderName,
        timestamp = timestamp ?: createdAt ?: fallbackTimestamp,
        type = resolvedType() ?: "text"
    )

    fun resolvedType(): String? =
        type ?: messageType ?: contentType ?: mediaType ?: attachments.orEmpty().firstOrNull()?.inferAttachmentType()
}

fun displayMessagePreviewContent(
    content: String?,
    type: String?,
    isUnsent: Boolean,
    attachments: List<String>? = emptyList()
): String {
    if (isUnsent) return "Message unsent"
    if (!content.isNullOrBlank()) return content
    return when ((type ?: attachments.orEmpty().firstOrNull()?.inferAttachmentType())?.lowercase()) {
        "image" -> "Image"
        "file" -> "File"
        "video" -> "Video"
        else -> ""
    }
}

private fun String.inferAttachmentType(): String {
    val path = substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".jpg") ||
            path.endsWith(".jpeg") ||
            path.endsWith(".png") ||
            path.endsWith(".gif") ||
            path.endsWith(".webp") ||
            path.endsWith(".bmp") ||
            path.endsWith(".heic") -> "image"
        path.endsWith(".mp4") ||
            path.endsWith(".mov") ||
            path.endsWith(".webm") ||
            path.endsWith(".mkv") -> "video"
        else -> "file"
    }
}
