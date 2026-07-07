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
            lastMessage = lastMessage?.toDomain()
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
    @SerializedName("handle") val handle: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String?
) {
    fun toDomain(): ParticipantPreview = ParticipantPreview(
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
}

data class MessagePreviewDto(
    @SerializedName("id") val id: String,
    @SerializedName("content") val content: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("senderName") val senderName: String?,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("type") val type: String
) {
    fun toDomain(): MessagePreview = MessagePreview(
        id = id,
        content = content,
        senderId = senderId,
        senderName = senderName,
        timestamp = timestamp,
        type = type
    )
}
