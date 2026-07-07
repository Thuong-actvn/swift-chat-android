package com.thuo_ng.swift_chat_android.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.thuo_ng.swift_chat_android.data.local.entity.NotificationEntity
import com.thuo_ng.swift_chat_android.domain.model.Notification
import com.thuo_ng.swift_chat_android.domain.model.NotificationActor
import com.thuo_ng.swift_chat_android.domain.model.NotificationPage
import com.thuo_ng.swift_chat_android.domain.model.normalizeNotificationType

data class NotificationResponseDto(
    @SerializedName("data") val data: List<NotificationDto>,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int? = null,
    @SerializedName("hasMore") val hasMore: Boolean? = null,
    @SerializedName("nextCursor") val nextCursor: String? = null
) {
    fun toDomain(): NotificationPage = NotificationPage(
        notifications = data.map { it.toDomain() },
        total = total ?: data.size,
        limit = limit ?: data.size,
        offset = offset ?: 0,
        hasMore = hasMore ?: false,
        nextCursor = nextCursor
    )
}

data class NotificationDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("referenceId") val referenceId: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("actor") val actor: NotificationActorDto? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("payload") val payload: Any? = null
) {
    fun toDomain(): Notification {
        val display = resolveNotificationCopy(type = type, actorName = actor?.displayName)
        return Notification(
            id = id,
            type = type,
            referenceId = referenceId,
            isRead = isRead,
            createdAt = createdAt,
            actor = actor?.toDomain(),
            title = title ?: display.first,
            message = message ?: display.second,
            payloadJson = payload?.toString()
        )
    }

    fun toEntity(): NotificationEntity {
        val domain = toDomain()
        return NotificationEntity(
            id = domain.id,
            type = domain.type,
            referenceId = domain.referenceId,
            isRead = domain.isRead,
            createdAt = domain.createdAt,
            actorId = domain.actor?.id,
            actorHandle = domain.actor?.handle,
            actorDisplayName = domain.actor?.displayName,
            actorAvatarUrl = domain.actor?.avatarUrl,
            title = domain.title,
            message = domain.message,
            payloadJson = domain.payloadJson
        )
    }
}

data class NotificationActorDto(
    @SerializedName("id") val id: String?,
    @SerializedName("handle") val handle: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?
) {
    fun toDomain(): NotificationActor = NotificationActor(
        id = id,
        handle = handle,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
}

data class NotificationUnreadCountDto(
    @SerializedName("unreadCount") val unreadCount: Int
)

data class NotificationActionResponseDto(
    @SerializedName("success") val success: Boolean
)

fun NotificationEntity.toDomain(): Notification = Notification(
    id = id,
    type = type,
    referenceId = referenceId,
    isRead = isRead,
    createdAt = createdAt,
    actor = if (actorId == null && actorHandle == null && actorDisplayName == null && actorAvatarUrl == null) {
        null
    } else {
        NotificationActor(
            id = actorId,
            handle = actorHandle,
            displayName = actorDisplayName,
            avatarUrl = actorAvatarUrl
        )
    },
    title = title,
    message = message,
    payloadJson = payloadJson
)

fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    type = type,
    referenceId = referenceId,
    isRead = isRead,
    createdAt = createdAt,
    actorId = actor?.id,
    actorHandle = actor?.handle,
    actorDisplayName = actor?.displayName,
    actorAvatarUrl = actor?.avatarUrl,
    title = title,
    message = message,
    payloadJson = payloadJson
)

fun NotificationPage.toResponse(): NotificationPage = this

private fun resolveNotificationCopy(type: String, actorName: String?): Pair<String, String> {
    val name = actorName?.takeIf { it.isNotBlank() }
    return when (type.normalizeNotificationType()) {
        "friend_request_received" -> {
            val title = if (name != null) "$name sent you a friend request." else "New friend request"
            title to "Tap to review this request."
        }
        "friend_request_accepted" -> {
            val title = if (name != null) "$name accepted your friend request." else "Friend request accepted"
            title to "You are now connected."
        }
        "added_to_group" -> {
            val title = if (name != null) "$name added you to a group." else "Added to a group"
            title to "You were added to a new group conversation."
        }
        "removed_from_group" -> {
            val title = if (name != null) "$name removed you from a group." else "Removed from a group"
            title to "You no longer have access to that group."
        }
        "group_role_changed" -> {
            val title = if (name != null) "$name changed your group role." else "Group role changed"
            title to "Your role in a group has been updated."
        }
        "new_message" -> {
            val title = if (name != null) "$name sent you a message." else "New message"
            title to "Open the conversation to read it."
        }
        else -> {
            val fallback = if (name != null) "$name sent an update." else "New notification"
            fallback to "Open to view details."
        }
    }
}