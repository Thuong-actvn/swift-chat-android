package com.thuo_ng.swift_chat_android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.model.Notification
import com.thuo_ng.swift_chat_android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

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
        refreshNotifications(isRefresh = false)
    }

    fun handleIntent(intent: NotificationIntent) {
        when (intent) {
            NotificationIntent.Refresh -> refreshNotifications(isRefresh = true)
            NotificationIntent.MarkAllAsRead -> markAllAsRead()
            is NotificationIntent.NotificationClicked -> onNotificationClicked(intent.notification)
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationRepository.observeNotifications().collect { notifications ->
                _uiState.update { current ->
                    current.copy(
                        notifications = notifications,
                        sections = groupNotifications(notifications),
                        isLoading = if (notifications.isNotEmpty()) false else current.isLoading,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.observeUnreadCount().collect { unreadCount ->
                _uiState.update { it.copy(unreadCount = unreadCount) }
            }
        }
    }

    private fun refreshNotifications(isRefresh: Boolean) {
        if (isRefresh && _uiState.value.isRefreshing) return

        viewModelScope.launch {
            val shouldShowInitialLoading = !isRefresh && _uiState.value.notifications.isEmpty()
            _uiState.update {
                it.copy(
                    isLoading = shouldShowInitialLoading,
                    isRefreshing = isRefresh,
                    errorMessage = null
                )
            }

            when (val result = notificationRepository.syncNotifications()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                    _effect.send(NotificationEffect.ShowMessage(result.message))
                }
            }

            when (val unreadResult = notificationRepository.syncUnreadCount()) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(unreadCount = unreadResult.data)
                }
                is NetworkResult.Error -> Unit
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            val result = notificationRepository.markAllAsRead()
            if (result.isSuccess) {
                _uiState.update { it.copy(unreadCount = 0) }
                _effect.send(NotificationEffect.ShowMessage("All notifications marked as read"))
            } else {
                val message = result.exceptionOrNull()?.message ?: "Could not mark notifications as read"
                _effect.send(NotificationEffect.ShowMessage(message))
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

            if (notification.type == "new_message" && notification.referenceId != null) {
                _effect.send(NotificationEffect.OpenConversation(notification.referenceId))
            }
        }
    }

    private fun groupNotifications(notifications: List<Notification>): List<NotificationSection> {
        val zoneId = ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        return notifications
            .sortedByDescending { parseInstant(it.createdAt) }
            .groupBy { notification ->
                val notificationDate = Instant.ofEpochMilli(parseInstant(notification.createdAt).toEpochMilli())
                    .atZone(zoneId)
                    .toLocalDate()
                when (notificationDate) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> notificationDate.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
                    )
                }
            }
            .map { (label, items) ->
                NotificationSection(label = label, items = items)
            }
    }

    private fun parseInstant(createdAt: String): Instant {
        return runCatching { Instant.parse(createdAt) }
            .getOrElse {
                runCatching { java.time.OffsetDateTime.parse(createdAt).toInstant() }
                    .getOrElse { Instant.EPOCH }
            }
    }
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val sections: List<NotificationSection> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
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
}