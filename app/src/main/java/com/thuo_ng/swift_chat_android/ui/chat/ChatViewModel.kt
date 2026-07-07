package com.thuo_ng.swift_chat_android.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val secureStorage: SecureStorage,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(currentAccountId = secureStorage.getUserId()))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect: Flow<ChatEffect> = _effect.receiveAsFlow()

    private var conversationJob: Job? = null
    private var messagesJob: Job? = null
    private var readReceiptsJob: Job? = null
    private var typingUsersJob: Job? = null
    private var typingStopJob: Job? = null
    private var activeConversationId: String? = null
    private var lastMarkedReadMessageId: String? = null
    private var lastTypingEmitAt: Long = 0L

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.Start -> start(intent.conversationId)
            is ChatIntent.InputChanged -> handleInputChanged(intent.value)
            is ChatIntent.InputFocusChanged -> handleFocusChanged(intent.focused)
            ChatIntent.SendClicked -> sendCurrentText()
            is ChatIntent.AttachmentSelected -> sendAttachment(intent.uri, intent.type)
            ChatIntent.LoadOlder -> loadOlder()
            is ChatIntent.RetryMessage -> retryMessage(intent.message)
            is ChatIntent.UnsendMessage -> unsendMessage(intent.message)
            is ChatIntent.DeleteForMe -> deleteForMe(intent.message)
            is ChatIntent.ToggleReaction -> toggleReaction(intent.message, intent.emoji)
            is ChatIntent.TogglePin -> togglePin(intent.message)
        }
    }

    private fun start(conversationId: String) {
        if (activeConversationId == conversationId) return

        activeConversationId?.let { chatRepository.leaveRoom(it) }
        activeConversationId = conversationId
        lastMarkedReadMessageId = null
        chatRepository.joinRoom(conversationId)

        _uiState.update {
            it.copy(
                conversationId = conversationId,
                currentAccountId = secureStorage.getUserId(),
                isInitialSyncing = true,
                hasMoreOlderMessages = true,
                errorMessage = null
            )
        }

        conversationJob?.cancel()
        conversationJob = viewModelScope.launch {
            chatRepository.observeConversation(conversationId).collect { conversation ->
                _uiState.update { it.copy(conversation = conversation) }
            }
        }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
                markLatestIncomingMessageRead(messages)
            }
        }

        readReceiptsJob?.cancel()
        readReceiptsJob = viewModelScope.launch {
            chatRepository.observeReadReceipts(conversationId).collect { receipts ->
                _uiState.update { it.copy(readReceipts = receipts) }
            }
        }

        typingUsersJob?.cancel()
        typingUsersJob = viewModelScope.launch {
            chatRepository.observeTypingUsers(conversationId).collect { users ->
                _uiState.update { it.copy(typingUsers = users) }
            }
        }

        viewModelScope.launch {
            when (val result = chatRepository.syncLatestMessages(conversationId)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isInitialSyncing = false,
                        errorMessage = null,
                        hasMoreOlderMessages = result.data >= MESSAGE_PAGE_SIZE
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isInitialSyncing = false, errorMessage = result.message)
                    }
                    if (result.code != 429) {
                        _effect.send(ChatEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    private fun handleInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value) }
        val conversationId = activeConversationId ?: return
        if (value.isBlank()) {
            stopTyping(conversationId)
        } else if (_uiState.value.isInputFocused) {
            emitTypingThrottled(conversationId)
        }
    }

    private fun handleFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isInputFocused = focused) }
        val conversationId = activeConversationId ?: return
        if (focused && _uiState.value.inputText.isNotBlank()) {
            emitTypingThrottled(conversationId)
        } else if (!focused) {
            stopTyping(conversationId)
        }
    }

    private fun sendCurrentText() {
        val conversationId = activeConversationId ?: return
        val text = _uiState.value.inputText
        if (text.isBlank()) return

        _uiState.update { it.copy(inputText = "") }
        stopTyping(conversationId)

        viewModelScope.launch {
            chatRepository.sendTextMessage(conversationId, text)
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not send message")) }
        }
    }

    private fun sendAttachment(uri: android.net.Uri, type: String) {
        val conversationId = activeConversationId ?: return
        viewModelScope.launch {
            chatRepository.sendAttachmentMessage(conversationId, uri, type, appContext)
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not send attachment")) }
        }
    }

    private fun loadOlder() {
        val conversationId = activeConversationId ?: return
        val state = _uiState.value
        if (state.isLoadingOlder || state.isInitialSyncing || !state.hasMoreOlderMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOlder = true) }
            when (val result = chatRepository.loadOlderMessages(conversationId)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoadingOlder = false, hasMoreOlderMessages = result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingOlder = false, errorMessage = result.message) }
                    if (result.code != 429) {
                        _effect.send(ChatEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    private fun retryMessage(message: Message) {
        viewModelScope.launch {
            chatRepository.retryMessage(message)
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not retry message")) }
        }
    }

    private fun unsendMessage(message: Message) {
        viewModelScope.launch {
            chatRepository.unsendMessage(message)
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not unsend message")) }
        }
    }

    private fun deleteForMe(message: Message) {
        viewModelScope.launch {
            chatRepository.deleteForMe(message)
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not delete message")) }
        }
    }

    private fun toggleReaction(message: Message, emoji: String) {
        viewModelScope.launch {
            chatRepository.toggleReaction(message, emoji)
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not update reaction")) }
        }
    }

    private fun togglePin(message: Message) {
        viewModelScope.launch {
            val result = if (message.isPinned) {
                chatRepository.unpinMessage(message)
            } else {
                chatRepository.pinMessage(message)
            }
            result.onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not update pin")) }
        }
    }

    private fun markLatestIncomingMessageRead(messages: List<Message>) {
        val conversationId = activeConversationId ?: return
        val currentAccountId = secureStorage.getUserId() ?: return
        val latestIncoming = messages
            .lastOrNull { message ->
                message.senderId != currentAccountId &&
                    !message.isDeleted &&
                    message.serverId != null
            } ?: return
        val serverId = latestIncoming.serverId ?: return
        if (serverId == lastMarkedReadMessageId) return

        lastMarkedReadMessageId = serverId
        viewModelScope.launch {
            chatRepository.markRead(conversationId, serverId)
        }
    }

    private fun emitTypingThrottled(conversationId: String) {
        val now = System.currentTimeMillis()
        if (now - lastTypingEmitAt >= TYPING_THROTTLE_MS) {
            lastTypingEmitAt = now
            chatRepository.sendTyping(conversationId)
        }
        typingStopJob?.cancel()
        typingStopJob = viewModelScope.launch {
            delay(TYPING_IDLE_STOP_MS)
            stopTyping(conversationId)
        }
    }

    private fun stopTyping(conversationId: String) {
        typingStopJob?.cancel()
        typingStopJob = null
        lastTypingEmitAt = 0L
        chatRepository.sendStopTyping(conversationId)
    }

    override fun onCleared() {
        activeConversationId?.let { chatRepository.leaveRoom(it) }
        typingStopJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TYPING_THROTTLE_MS = 2_500L
        const val TYPING_IDLE_STOP_MS = 1_500L
        const val MESSAGE_PAGE_SIZE = 50
    }
}
