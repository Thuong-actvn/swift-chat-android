package com.thuo_ng.swift_chat_android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.data.local.dao.NotificationDao
import com.thuo_ng.swift_chat_android.data.remote.api.NotificationApi
import com.thuo_ng.swift_chat_android.data.remote.dto.NotificationDto
import com.thuo_ng.swift_chat_android.data.remote.dto.toDomain
import com.thuo_ng.swift_chat_android.data.remote.dto.toEntity
import com.thuo_ng.swift_chat_android.domain.model.Notification
import com.thuo_ng.swift_chat_android.domain.model.NotificationPage
import com.thuo_ng.swift_chat_android.domain.model.normalizeNotificationType
import com.thuo_ng.swift_chat_android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi,
    private val notificationDao: NotificationDao
) : NotificationRepository {

    private val gson = Gson()

    override fun observeNotifications(): Flow<List<Notification>> {
        return notificationDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeUnreadCount(): Flow<Int> {
        return notificationDao.observeStoredUnreadCount()
            .combine(notificationDao.observeLocalUnreadCount()) { storedCount, localCount ->
                storedCount ?: localCount
            }
    }

    override fun observeNotification(id: String): Flow<Notification?> {
        return notificationDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun syncNotifications(): NetworkResult<NotificationPage> {
        return when (val result = safeApiCall { notificationApi.getNotifications() }) {
            is NetworkResult.Success -> {
                val page = parseNotificationsResponse(result.data)
                notificationDao.replaceAll(page.notifications.map { it.toEntity() })
                if (notificationDao.getStoredUnreadCount() == null) {
                    notificationDao.setUnreadCount(page.notifications.count { !it.isRead })
                }
                NetworkResult.Success(page)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun syncUnreadCount(): NetworkResult<Int> {
        return when (val result = safeApiCall { notificationApi.getUnreadCount() }) {
            is NetworkResult.Success -> {
                val unreadCount = parseUnreadCount(result.data).coerceAtLeast(0)
                notificationDao.setUnreadCount(unreadCount)
                NetworkResult.Success(unreadCount)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun markAsRead(id: String): Result<Unit> {
        return try {
            val response = notificationApi.markAsRead(id)
            if (response.isSuccessful) {
                val existing = notificationDao.getById(id)
                notificationDao.markAsRead(id)
                if (existing?.isRead == false) {
                    notificationDao.decrementUnreadCount()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Mark as read failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.isSuccessful) {
                notificationDao.markAllAsRead()
                notificationDao.setUnreadCount(0)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Mark all as read failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.NewNotification -> upsertNotification(
                id = event.id,
                type = event.type,
                referenceId = event.referenceId,
                isRead = event.isRead,
                createdAt = event.createdAt,
                actor = event.actor?.let {
                    com.thuo_ng.swift_chat_android.domain.model.NotificationActor(
                        id = it.id,
                        handle = it.username,
                        displayName = it.username,
                        avatarUrl = it.avatarUrl
                    )
                }
            )
            is SocketEvent.GroupMemberAdded -> upsertGeneratedNotification(
                id = socketNotificationKey("group_member_added", event.conversationId, event.addedBy),
                type = "added_to_group",
                referenceId = event.conversationId,
                actorName = event.addedBy
            )
            is SocketEvent.GroupMemberRemoved -> upsertGeneratedNotification(
                id = socketNotificationKey("group_member_removed", event.conversationId, event.removedBy),
                type = "removed_from_group",
                referenceId = event.conversationId,
                actorName = event.removedBy
            )
            is SocketEvent.GroupRoleChanged -> upsertGeneratedNotification(
                id = socketNotificationKey("group_role_changed", event.conversationId, event.changedBy, event.targetUserId, event.newRole),
                type = "group_role_changed",
                referenceId = event.conversationId,
                actorName = event.changedBy
            )
            is SocketEvent.GroupYouAdded -> upsertGeneratedNotification(
                id = socketNotificationKey("group_you_added", event.conversationId, event.addedBy),
                type = "added_to_group",
                referenceId = event.conversationId,
                actorName = event.addedBy
            )
            is SocketEvent.GroupInfoUpdated -> Unit
            is SocketEvent.GroupDisbanded -> upsertGeneratedNotification(
                id = socketNotificationKey("group_disbanded", event.conversationId, event.disbandedBy),
                type = "removed_from_group",
                referenceId = event.conversationId,
                actorName = event.disbandedBy
            )
            else -> Unit
        }
    }

    private suspend fun upsertNotification(
        id: String,
        type: String,
        referenceId: String?,
        isRead: Boolean,
        createdAt: String,
        actor: com.thuo_ng.swift_chat_android.domain.model.NotificationActor?
    ) {
        val notification = Notification(
            id = id,
            type = type,
            referenceId = referenceId,
            isRead = isRead,
            createdAt = createdAt,
            actor = actor,
            title = resolveTitle(type, actor?.displayName),
            message = resolveMessage(type, actor?.displayName),
            payloadJson = null
        )
        upsertAndTrackUnread(notification)
    }

    private suspend fun upsertGeneratedNotification(
        id: String,
        type: String,
        referenceId: String?,
        actorName: String?
    ) {
        val now = java.time.Instant.now().toString()
        val notification = Notification(
            id = id,
            type = type,
            referenceId = referenceId,
            isRead = false,
            createdAt = now,
            actor = actorName?.let {
                com.thuo_ng.swift_chat_android.domain.model.NotificationActor(
                    id = null,
                    handle = it,
                    displayName = it,
                    avatarUrl = null
                )
            },
            title = resolveTitle(type, actorName),
            message = resolveMessage(type, actorName),
            payloadJson = null
        )
        upsertAndTrackUnread(notification)
    }

    private suspend fun upsertAndTrackUnread(notification: Notification) {
        val existing = notificationDao.getById(notification.id)
        notificationDao.upsert(notification.toEntity())
        when {
            existing?.isRead != false && !notification.isRead -> notificationDao.incrementUnreadCount()
            existing?.isRead == false && notification.isRead -> notificationDao.decrementUnreadCount()
        }
    }

    private fun resolveTitle(type: String, actorName: String?): String {
        val name = actorName?.takeIf { it.isNotBlank() }
        return when (type.normalizeNotificationType()) {
            "friend_request_received" -> name?.let { "$it sent you a friend request." } ?: "New friend request"
            "friend_request_accepted" -> name?.let { "$it accepted your friend request." } ?: "Friend request accepted"
            "added_to_group" -> name?.let { "$it added you to a group." } ?: "Added to a group"
            "removed_from_group" -> name?.let { "$it removed you from a group." } ?: "Removed from a group"
            "group_role_changed" -> name?.let { "$it changed your group role." } ?: "Group role changed"
            "new_message" -> name?.let { "$it sent you a message." } ?: "New message"
            else -> name?.let { "$it sent an update." } ?: "New notification"
        }
    }

    private fun resolveMessage(type: String, actorName: String?): String {
        return when (type.normalizeNotificationType()) {
            "friend_request_received" -> "Tap to review this request."
            "friend_request_accepted" -> "You are now connected."
            "added_to_group" -> "You were added to a new group conversation."
            "removed_from_group" -> "You no longer have access to that group."
            "group_role_changed" -> "Your role in a group has been updated."
            "new_message" -> "Open the conversation to read it."
            else -> "Open to view details."
        }
    }

    private fun socketNotificationKey(vararg parts: String): String {
        return parts.joinToString(separator = ":")
    }

    private fun parseNotificationsResponse(element: JsonElement): NotificationPage {
        val notifications = when {
            element.isJsonArray -> element.asJsonArray.map { gson.fromJson(it, NotificationDto::class.java).toDomain() }
            element.isJsonObject -> {
                val dataElement = element.asJsonObject.get("data")
                when {
                    dataElement != null && dataElement.isJsonArray -> {
                        dataElement.asJsonArray.map { gson.fromJson(it, NotificationDto::class.java).toDomain() }
                    }
                    dataElement != null && dataElement.isJsonObject -> listOf(gson.fromJson(dataElement, NotificationDto::class.java).toDomain())
                    else -> listOf(gson.fromJson(element, NotificationDto::class.java).toDomain())
                }
            }
            else -> emptyList()
        }

        return NotificationPage(
            notifications = notifications,
            total = notifications.size,
            limit = notifications.size,
            offset = 0,
            hasMore = false,
            nextCursor = null
        )
    }

    private fun parseUnreadCount(element: JsonElement): Int {
        return when {
            element.isJsonObject -> {
                val jsonObject = element.asJsonObject
                when {
                    jsonObject.has("unreadCount") -> jsonObject.get("unreadCount").asInt
                    jsonObject.has("count") -> jsonObject.get("count").asInt
                    jsonObject.has("data") && jsonObject.get("data").isJsonObject -> {
                        val dataObject = jsonObject.getAsJsonObject("data")
                        when {
                            dataObject.has("unreadCount") -> dataObject.get("unreadCount").asInt
                            dataObject.has("count") -> dataObject.get("count").asInt
                            else -> 0
                        }
                    }
                    else -> 0
                }
            }
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asInt
            else -> 0
        }
    }
}
