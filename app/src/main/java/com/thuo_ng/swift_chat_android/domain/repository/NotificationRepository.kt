package com.thuo_ng.swift_chat_android.domain.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.domain.model.Notification
import com.thuo_ng.swift_chat_android.domain.model.NotificationPage
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    fun observeNotification(id: String): Flow<Notification?>
    suspend fun syncNotifications(): NetworkResult<NotificationPage>
    suspend fun syncUnreadCount(): NetworkResult<Int>
    suspend fun markAsRead(id: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun handleSocketEvent(event: SocketEvent)
}