package com.thuo_ng.swift_chat_android.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.thuo_ng.swift_chat_android.core.session.SessionEvent
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.core.socket.SocketConnectionState
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.core.socket.SocketManager
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import com.thuo_ng.swift_chat_android.domain.repository.ChatRepository
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import com.thuo_ng.swift_chat_android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager,
    private val notificationRepository: NotificationRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private companion object {
        const val TAG = "AppViewModel"
    }

    val authState: StateFlow<AppAuthState> = authRepository.isLoggedInFlow
        .map { isLoggedIn ->
            if (isLoggedIn) AppAuthState.Authenticated else AppAuthState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppAuthState.Loading
        )
    private val _effect = Channel<AppEffect>(Channel.BUFFERED)
    val effect: Flow<AppEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessionEvent.collect { event ->
                when(event) {
                    SessionEvent.Expired ->
                        _effect.send(AppEffect.ShowMessage("Session expired. Please log in again."))
                    SessionEvent.LoggedOut -> Unit
                }
            }
        }
        
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collect { isLoggedIn ->
                if (isLoggedIn) {
                    socketManager.connect()
                } else {
                    socketManager.disconnect()
                }
            }
        }

        viewModelScope.launch {
            socketManager.events.collect { event ->
                runCatching {
                    when (event) {
                        is SocketEvent.ReceiveMessage,
                        is SocketEvent.UserTyping,
                        is SocketEvent.UserStopTyping,
                        is SocketEvent.MessageUnsent,
                        is SocketEvent.MessageDeletedForMe,
                        is SocketEvent.MessageEdited,
                        is SocketEvent.ReadReceipt,
                        is SocketEvent.ReactionUpdated,
                        is SocketEvent.MessagePinned,
                        is SocketEvent.MessageUnpinned,
                        is SocketEvent.PresenceStatus -> chatRepository.handleSocketEvent(event)
                        is SocketEvent.NewNotification,
                        is SocketEvent.GroupInfoUpdated,
                        is SocketEvent.GroupMemberAdded,
                        is SocketEvent.GroupMemberRemoved,
                        is SocketEvent.GroupDisbanded,
                        is SocketEvent.GroupRoleChanged,
                        is SocketEvent.GroupYouAdded -> {
                            chatRepository.handleSocketEvent(event)
                            notificationRepository.handleSocketEvent(event)
                        }
                        else -> Unit
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Socket event handling failed: $event", error)
                }
            }
        }

        viewModelScope.launch {
            socketManager.connectionState.collect { state ->
                if (state is SocketConnectionState.Connected) {
                    conversationRepository.syncConversations()
                    notificationRepository.syncUnreadCount()
                }
            }
        }
    }
}
