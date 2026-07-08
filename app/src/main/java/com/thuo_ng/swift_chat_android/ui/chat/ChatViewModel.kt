package com.thuo_ng.swift_chat_android.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.DisplayInfo
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.SendStatus
import com.thuo_ng.swift_chat_android.domain.repository.ChatRepository
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.UUID
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
    private val conversationRepository: ConversationRepository,
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
    private var latestPersistedMessages: List<Message> = emptyList()
    private var pendingUiMessages: List<Message> = emptyList()
    private var pendingDirectPartnerId: String? = null

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.Start -> start(intent.conversationId)
            is ChatIntent.StartPendingDirect -> startPendingDirect(
                partnerId = intent.partnerId,
                displayName = intent.displayName,
                avatarUrl = intent.avatarUrl
            )
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

    private fun start(conversationId: String, keepPendingUiMessages: Boolean = false) {
        if (activeConversationId == conversationId) return

        activeConversationId?.let { chatRepository.leaveRoom(it) }
        cancelObservers()
        latestPersistedMessages = emptyList()
        if (!keepPendingUiMessages) {
            pendingUiMessages = emptyList()
            pendingDirectPartnerId = null
        }
        activeConversationId = conversationId
        lastMarkedReadMessageId = null
        chatRepository.joinRoom(conversationId)

        _uiState.update {
            it.copy(
                conversationId = conversationId,
                pendingDirect = null,
                currentAccountId = secureStorage.getUserId(),
                messages = mergedMessages(),
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
                latestPersistedMessages = messages
                publishMessages()
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

    private fun startPendingDirect(
        partnerId: String,
        displayName: String,
        avatarUrl: String?
    ) {
        if (pendingDirectPartnerId == partnerId && activeConversationId != null) return
        if (_uiState.value.pendingDirect?.partnerId == partnerId && activeConversationId == null) return

        activeConversationId?.let { chatRepository.leaveRoom(it) }
        activeConversationId = null
        pendingDirectPartnerId = partnerId
        lastMarkedReadMessageId = null
        cancelObservers()

        latestPersistedMessages = emptyList()
        pendingUiMessages = emptyList()

        val now = Instant.now().toString()
        _uiState.update {
            it.copy(
                conversationId = "",
                pendingDirect = PendingDirectChatInfo(
                    partnerId = partnerId,
                    displayName = displayName,
                    avatarUrl = avatarUrl
                ),
                conversation = Conversation(
                    id = "",
                    type = "direct",
                    displayInfo = DisplayInfo(
                        title = displayName,
                        avatarUrl = avatarUrl,
                        isOnline = null
                    ),
                    createdAt = now,
                    updatedAt = now,
                    unreadCount = 0,
                    currentParticipant = null,
                    participantPreview = emptyList(),
                    totalParticipants = 2,
                    lastMessage = null
                ),
                messages = emptyList(),
                readReceipts = emptyList(),
                typingUsers = emptyList(),
                currentAccountId = secureStorage.getUserId(),
                isInitialSyncing = false,
                isLoadingOlder = false,
                hasMoreOlderMessages = false,
                errorMessage = null
            )
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
        val text = _uiState.value.inputText
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val clientTempId = UUID.randomUUID().toString()
        val shouldBridgePendingDirect = activeConversationId == null && _uiState.value.pendingDirect != null
        if (shouldBridgePendingDirect) {
            appendPendingUiMessage(createPendingTextMessage(clientTempId, trimmed))
        }

        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            val conversationId = ensureConversationForSend(
                activateConversation = !shouldBridgePendingDirect,
                keepPendingUiMessages = shouldBridgePendingDirect
            ) ?: run {
                if (shouldBridgePendingDirect) {
                    removePendingUiMessage(clientTempId)
                }
                _uiState.update { it.copy(inputText = text) }
                return@launch
            }
            if (shouldBridgePendingDirect) {
                updatePendingUiMessageConversation(clientTempId, conversationId)
            }
            stopTyping(conversationId)
            chatRepository.sendTextMessage(conversationId, text, clientTempId)
                .onSuccess {
                    if (shouldBridgePendingDirect) {
                        start(conversationId, keepPendingUiMessages = true)
                        delay(NEW_DIRECT_SYNC_DELAY_MS)
                    }
                    conversationRepository.syncConversations(
                        preserveConversationIds = setOf(conversationId)
                    )
                }
                .onFailure { error ->
                    if (shouldBridgePendingDirect && latestPersistedMessages.none { it.clientTempId == clientTempId }) {
                        markPendingUiMessageFailed(clientTempId)
                    }
                    if (shouldBridgePendingDirect) {
                        start(conversationId, keepPendingUiMessages = true)
                    }
                    _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not send message"))
                }
        }
    }

    private fun sendAttachment(uri: android.net.Uri, type: String) {
        viewModelScope.launch {
            val wasPendingDirect = activeConversationId == null && _uiState.value.pendingDirect != null
            val conversationId = ensureConversationForSend() ?: return@launch
            chatRepository.sendAttachmentMessage(conversationId, uri, type, appContext)
                .onSuccess {
                    if (wasPendingDirect) {
                        delay(NEW_DIRECT_SYNC_DELAY_MS)
                    }
                    conversationRepository.syncConversations(
                        preserveConversationIds = setOf(conversationId)
                    )
                }
                .onFailure { error -> _effect.send(ChatEffect.ShowMessage(error.message ?: "Could not send attachment")) }
        }
    }

    private suspend fun ensureConversationForSend(
        activateConversation: Boolean = true,
        keepPendingUiMessages: Boolean = false
    ): String? {
        activeConversationId?.let { return it }
        val pending = _uiState.value.pendingDirect ?: return null

        _uiState.update { it.copy(isInitialSyncing = true, errorMessage = null) }
        return when (val result = conversationRepository.openOrCreateDirectConversation(pending.partnerId)) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(
                        pendingDirect = null,
                        isInitialSyncing = false,
                        errorMessage = null
                    )
                }
                if (activateConversation) {
                    start(result.data.id, keepPendingUiMessages = keepPendingUiMessages)
                } else {
                    _uiState.update {
                        it.copy(
                            conversationId = result.data.id,
                            conversation = result.data,
                            messages = mergedMessages(),
                            isInitialSyncing = true,
                            errorMessage = null
                        )
                    }
                }
                result.data.id
            }
            is NetworkResult.Error -> {
                _uiState.update {
                    it.copy(isInitialSyncing = false, errorMessage = result.message)
                }
                _effect.send(ChatEffect.ShowMessage(result.message))
                null
            }
        }
    }

    private fun createPendingTextMessage(clientTempId: String, content: String): Message {
        val now = Instant.now().toString()
        return Message(
            localId = clientTempId,
            serverId = null,
            clientTempId = clientTempId,
            conversationId = activeConversationId.orEmpty(),
            senderId = secureStorage.getUserId().orEmpty(),
            sender = null,
            content = content,
            type = "text",
            createdAt = now,
            updatedAt = null,
            isUnsent = false,
            isEdited = false,
            isDeleted = false,
            isPinned = false,
            pinnedBy = null,
            pinnedAt = null,
            replyTo = null,
            forwardedFrom = null,
            reactions = emptyList(),
            attachments = emptyList(),
            sendStatus = SendStatus.SENDING
        )
    }

    private fun appendPendingUiMessage(message: Message) {
        pendingUiMessages = pendingUiMessages + message
        publishMessages()
    }

    private fun updatePendingUiMessageConversation(clientTempId: String, conversationId: String) {
        pendingUiMessages = pendingUiMessages.map { message ->
            if (message.clientTempId == clientTempId) {
                message.copy(conversationId = conversationId)
            } else {
                message
            }
        }
        publishMessages()
    }

    private fun markPendingUiMessageFailed(clientTempId: String) {
        pendingUiMessages = pendingUiMessages.map { message ->
            if (message.clientTempId == clientTempId) {
                message.copy(sendStatus = SendStatus.FAILED)
            } else {
                message
            }
        }
        publishMessages()
    }

    private fun removePendingUiMessage(clientTempId: String) {
        pendingUiMessages = pendingUiMessages.filterNot { it.clientTempId == clientTempId }
        publishMessages()
    }

    private fun publishMessages() {
        val persistedClientTempIds = latestPersistedMessages.mapNotNull { it.clientTempId }.toSet()
        if (persistedClientTempIds.isNotEmpty()) {
            pendingUiMessages = pendingUiMessages.filterNot { it.clientTempId in persistedClientTempIds }
        }
        _uiState.update { it.copy(messages = mergedMessages()) }
    }

    private fun mergedMessages(): List<Message> {
        return (latestPersistedMessages + pendingUiMessages)
            .distinctBy { it.clientTempId ?: it.localId }
            .sortedBy { it.createdAt }
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
        cancelObservers()
        typingStopJob?.cancel()
        super.onCleared()
    }

    private fun cancelObservers() {
        conversationJob?.cancel()
        conversationJob = null
        messagesJob?.cancel()
        messagesJob = null
        readReceiptsJob?.cancel()
        readReceiptsJob = null
        typingUsersJob?.cancel()
        typingUsersJob = null
    }

    private companion object {
        const val TYPING_THROTTLE_MS = 2_500L
        const val TYPING_IDLE_STOP_MS = 1_500L
        const val NEW_DIRECT_SYNC_DELAY_MS = 1_000L
        const val MESSAGE_PAGE_SIZE = 50
    }
}
