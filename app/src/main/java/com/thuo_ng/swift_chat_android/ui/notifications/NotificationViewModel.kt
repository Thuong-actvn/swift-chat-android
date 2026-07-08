package com.thuo_ng.swift_chat_android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.model.Notification
import com.thuo_ng.swift_chat_android.domain.model.isFriendRequestReceivedType
import com.thuo_ng.swift_chat_android.domain.model.isNewMessageType
import com.thuo_ng.swift_chat_android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _effect = Channel<NotificationEffect>(Channel.BUFFERED)
    val effect: Flow<NotificationEffect> = _effect.receiveAsFlow()

    init {
        observeNotifications()
        observeUnreadCount()
        refreshNotifications(false)
    }

    fun handleIntent(intent: NotificationIntent) {
        when (intent) {
            NotificationIntent.Refresh -> refreshNotifications(true)
            NotificationIntent.MarkAllAsRead -> markAllAsRead()
            is NotificationIntent.NotificationClicked -> onNotificationClicked(intent.notification)
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationRepository.observeNotifications().collect { list ->
                _uiState.update { 
                    it.copy(
                        notifications = list,
                        sections = groupNotifications(list)
                    )
                }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.observeUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    private fun refreshNotifications(isManual: Boolean) {
        viewModelScope.launch {
            if (isManual) _uiState.update { it.copy(isRefreshing = true) }
            else _uiState.update { it.copy(isLoading = true) }

            val result = notificationRepository.syncNotifications()
            if (result is NetworkResult.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
                _effect.send(NotificationEffect.ShowMessage(result.message))
            } else {
                _uiState.update { it.copy(errorMessage = null) }
                notificationRepository.syncUnreadCount()
            }

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message ?: "Could not mark all as read"
                _effect.send(NotificationEffect.ShowMessage(message))
            } else {
                notificationRepository.syncUnreadCount()
            }
        }
    }

    private fun onNotificationClicked(notification: Notification) {
        viewModelScope.launch {
            if (!notification.isRead) {
                val result = notificationRepository.markAsRead(notification.id)
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: "Could not mark notification as read"
                    _effect.send(NotificationEffect.ShowMessage(message))
                }
            }

            val type = notification.type
            if (type.isNewMessageType() && notification.referenceId != null) {
                _effect.send(NotificationEffect.OpenConversation(notification.referenceId))
            } else if (type.isFriendRequestReceivedType()) {
                _effect.send(NotificationEffect.OpenFriendsReceived)
            }
        }
    }

    private fun groupNotifications(list: List<Notification>): List<NotificationSection> {
        val now = Instant.now()
        val today = list.filter { 
            parseInstant(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() == 
                now.atZone(ZoneId.systemDefault()).toLocalDate() 
        }
        val earlier = list.filter { 
            parseInstant(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() != 
                now.atZone(ZoneId.systemDefault()).toLocalDate() 
        }

        val sections = mutableListOf<NotificationSection>()
        if (today.isNotEmpty()) sections.add(NotificationSection("Today", today))
        if (earlier.isNotEmpty()) sections.add(NotificationSection("Earlier", earlier))
        
        return sections
    }

    private fun parseInstant(dateStr: String): Instant {
        return try {
            Instant.parse(dateStr)
        } catch (e: Exception) {
            Instant.now()
        }
    }
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val sections: List<NotificationSection> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

data class NotificationSection(
    val label: String,
    val items: List<Notification>
)

sealed interface NotificationIntent {
    data object Refresh : NotificationIntent
    data object MarkAllAsRead : NotificationIntent
    data class NotificationClicked(val notification: Notification) : NotificationIntent
}

sealed interface NotificationEffect {
    data class ShowMessage(val message: String) : NotificationEffect
    data class OpenConversation(val conversationId: String) : NotificationEffect
    data object OpenFriendsReceived : NotificationEffect
}
