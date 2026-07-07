package com.thuo_ng.swift_chat_android.domain.model

data class Notification(
    val id: String,
    val type: String,
    val referenceId: String?,
    val isRead: Boolean,
    val createdAt: String,
    val actor: NotificationActor?,
    val title: String,
    val message: String,
    val payloadJson: String?
)

data class NotificationActor(
    val id: String?,
    val handle: String?,
    val displayName: String?,
    val avatarUrl: String?
)

data class NotificationPage(
    val notifications: List<Notification>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
    val nextCursor: String?
)