package com.thuo_ng.swift_chat_android.data.repository

import android.content.Context
import android.net.Uri
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.core.socket.EditMessagePayload
import com.thuo_ng.swift_chat_android.core.socket.MarkReadPayload
import com.thuo_ng.swift_chat_android.core.socket.MessagePayload
import com.thuo_ng.swift_chat_android.core.socket.ReactPayload
import com.thuo_ng.swift_chat_android.core.socket.SendMessagePayload
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.core.socket.SocketManager
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.local.dao.ConversationDao
import com.thuo_ng.swift_chat_android.data.local.dao.MessageDao
import com.thuo_ng.swift_chat_android.data.local.dao.ReadReceiptDao
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageReactionEntity
import com.thuo_ng.swift_chat_android.data.local.relation.ConversationWithParticipantPreviews
import com.thuo_ng.swift_chat_android.data.mapper.normalizedAccountId
import com.thuo_ng.swift_chat_android.data.mapper.optimisticMessageGraph
import com.thuo_ng.swift_chat_android.data.mapper.toDomain
import com.thuo_ng.swift_chat_android.data.mapper.toEntity
import com.thuo_ng.swift_chat_android.data.mapper.toEntityGraph
import com.thuo_ng.swift_chat_android.data.mapper.toParticipantPreviewEntities
import com.thuo_ng.swift_chat_android.data.remote.api.ConversationApi
import com.thuo_ng.swift_chat_android.data.remote.api.MessageApi
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationMemberDto
import com.thuo_ng.swift_chat_android.data.remote.dto.MessageDto
import com.thuo_ng.swift_chat_android.data.remote.dto.displayMessagePreviewContent
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.ReadReceipt
import com.thuo_ng.swift_chat_android.domain.model.SendStatus
import com.thuo_ng.swift_chat_android.domain.model.TypingUser
import com.thuo_ng.swift_chat_android.domain.model.isNewMessageType
import com.thuo_ng.swift_chat_android.domain.repository.ChatRepository
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val conversationApi: ConversationApi,
    private val messageDao: MessageDao,
    private val readReceiptDao: ReadReceiptDao,
    private val conversationDao: ConversationDao,
    private val socketManager: SocketManager,
    private val secureStorage: SecureStorage,
    private val userRepository: UserRepository
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeConversationId = MutableStateFlow<String?>(null)
    private val typingUsersByConversation = MutableStateFlow<Map<String, List<TypingUser>>>(emptyMap())
    private val typingExpiryJobs = mutableMapOf<String, Job>()

    override fun observeConversation(conversationId: String): Flow<Conversation?> {
        return conversationDao.observeById(conversationId).map { it?.toDomain() }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.observeMessages(conversationId).map { messages ->
            messages.map { it.toDomain() }
        }
    }

    override fun observeReadReceipts(conversationId: String): Flow<List<ReadReceipt>> {
        return readReceiptDao.observeReadReceipts(conversationId).map { receipts ->
            receipts.map { it.toDomain() }
        }
    }

    override fun observeTypingUsers(conversationId: String): Flow<List<TypingUser>> {
        return typingUsersByConversation.map { it[conversationId].orEmpty() }
    }

    override suspend fun syncLatestMessages(conversationId: String): NetworkResult<Int> {
        if (!conversationDao.exists(conversationId)) {
            return NetworkResult.Error(message = "Conversation is not available")
        }
        syncConversationMembers(conversationId)
        return when (val result = safeApiCall { messageApi.getMessages(conversationId = conversationId) }) {
            is NetworkResult.Success -> {
                if (!conversationDao.exists(conversationId)) {
                    return NetworkResult.Error(message = "Conversation is not available")
                }
                persistMessageDtos(result.data)
                updateConversationPreviewFromLatestMessage(conversationId, result.data)
                NetworkResult.Success(result.data.size)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun loadOlderMessages(conversationId: String): NetworkResult<Boolean> {
        if (!conversationDao.exists(conversationId)) {
            return NetworkResult.Error(message = "Conversation is not available")
        }
        val cursor = messageDao.getOldestServerMessageId(conversationId)
        return when (val result = safeApiCall { messageApi.getMessages(conversationId = conversationId, cursor = cursor) }) {
            is NetworkResult.Success -> {
                if (!conversationDao.exists(conversationId)) {
                    return NetworkResult.Error(message = "Conversation is not available")
                }
                persistMessageDtos(result.data)
                NetworkResult.Success(result.data.size >= MESSAGE_PAGE_SIZE)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun sendTextMessage(
        conversationId: String,
        content: String,
        clientTempId: String?
    ): Result<Unit> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)
        return sendOptimisticMessage(
            conversationId = conversationId,
            content = trimmed,
            type = "text",
            attachments = emptyList(),
            clientTempId = clientTempId ?: UUID.randomUUID().toString()
        )
    }

    override suspend fun sendAttachmentMessage(
        conversationId: String,
        uri: Uri,
        type: String,
        context: Context
    ): Result<Unit> {
        val uploadResult = userRepository.uploadFile(uri, context)
        val url = uploadResult.getOrElse { return Result.failure(it) }
        val messageType = if (type.equals("image", ignoreCase = true)) "image" else "file"
        return sendOptimisticMessage(
            conversationId = conversationId,
            content = "",
            type = messageType,
            attachments = listOf(url)
        )
    }

    override suspend fun retryMessage(message: Message): Result<Unit> {
        val clientTempId = message.clientTempId ?: message.localId
        messageDao.updateSendStatus(clientTempId, SendStatus.SENDING.name)
        return emitPendingMessage(
            conversationId = message.conversationId,
            content = message.content,
            type = message.type,
            clientTempId = clientTempId,
            attachments = message.attachments.map { it.url }
        )
    }

    override suspend fun editMessage(message: Message, content: String): Result<Unit> {
        val messageId = message.serverId ?: return Result.failure(IllegalStateException("Message is not synced yet"))
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)

        return if (socketManager.editMessage(EditMessagePayload(messageId, message.conversationId, trimmed))) {
            messageDao.updateEditedMessage(messageId, trimmed, Instant.now().toString())
            refreshConversationPreviewFromLatestMessage(message.conversationId)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Socket is not connected"))
        }
    }

    override suspend fun unsendMessage(message: Message): Result<Unit> {
        val messageId = message.serverId ?: return Result.failure(IllegalStateException("Message is not synced yet"))
        return if (socketManager.unsendMessage(messageId, message.conversationId)) {
            messageDao.markUnsent(messageId, Instant.now().toString())
            refreshConversationPreviewFromLatestMessage(message.conversationId)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Socket is not connected"))
        }
    }

    override suspend fun deleteForMe(message: Message): Result<Unit> {
        val messageId = message.serverId ?: message.localId
        val emitted = message.serverId == null || socketManager.deleteForMe(messageId, message.conversationId)
        return if (emitted) {
            messageDao.deleteForMe(messageId)
            refreshConversationPreviewFromLatestMessage(message.conversationId)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Socket is not connected"))
        }
    }

    override suspend fun toggleReaction(message: Message, emoji: String): Result<Unit> {
        val messageId = message.serverId ?: return Result.failure(IllegalStateException("Message is not synced yet"))
        return if (socketManager.reactMessage(ReactPayload(messageId, message.conversationId, emoji))) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Socket is not connected"))
        }
    }

    override suspend fun pinMessage(message: Message): Result<Unit> {
        val messageId = message.serverId ?: return Result.failure(IllegalStateException("Message is not synced yet"))
        return if (socketManager.pinMessage(messageId, message.conversationId)) {
            messageDao.markPinned(messageId, secureStorage.getUserId(), Instant.now().toString())
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Socket is not connected"))
        }
    }

    override suspend fun unpinMessage(message: Message): Result<Unit> {
        val messageId = message.serverId ?: return Result.failure(IllegalStateException("Message is not synced yet"))
        return if (socketManager.unpinMessage(messageId, message.conversationId)) {
            messageDao.markUnpinned(messageId)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Socket is not connected"))
        }
    }

    override suspend fun markRead(conversationId: String, messageId: String) {
        val accountId = secureStorage.getUserId() ?: return
        if (!conversationDao.exists(conversationId)) return
        socketManager.markRead(MarkReadPayload(conversationId, messageId))
        conversationDao.markCurrentParticipantRead(conversationId, messageId)
        readReceiptDao.markRead(conversationId, accountId, messageId)
    }

    override fun joinRoom(conversationId: String) {
        activeConversationId.value = conversationId
        socketManager.joinRoom(conversationId)
    }

    override fun leaveRoom(conversationId: String) {
        if (activeConversationId.value == conversationId) {
            activeConversationId.value = null
        }
        sendStopTyping(conversationId)
        socketManager.leaveRoom(conversationId)
    }

    override fun sendTyping(conversationId: String) {
        socketManager.sendTyping(conversationId)
    }

    override fun sendStopTyping(conversationId: String) {
        socketManager.sendStopTyping(conversationId)
    }

    override suspend fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.ReceiveMessage -> handleReceiveMessage(event)
            is SocketEvent.NewNotification -> handleNewMessageNotification(event)
            is SocketEvent.MessageEdited -> {
                messageDao.updateEditedMessage(
                    messageId = event.messageId,
                    content = event.content,
                    updatedAt = event.timestamp
                )
                refreshConversationPreviewAfterMessageMutation(event.conversationId, event.messageId)
            }
            is SocketEvent.MessageUnsent -> {
                messageDao.markUnsent(event.messageId, event.timestamp)
                refreshConversationPreviewAfterMessageMutation(event.conversationId, event.messageId)
            }
            is SocketEvent.MessageDeletedForMe -> {
                messageDao.deleteForMe(event.messageId)
                refreshConversationPreviewAfterMessageMutation(event.conversationId, event.messageId)
            }
            is SocketEvent.ReactionUpdated -> messageDao.replaceReactions(
                messageId = event.messageId,
                reactions = event.reactions.orEmpty().flatMap { reaction ->
                    if (reaction.emoji.isBlank()) return@flatMap emptyList()
                    reaction.accountIds().map { accountId ->
                        MessageReactionEntity(
                            messageLocalId = event.messageId,
                            emoji = reaction.emoji,
                            accountId = accountId,
                            createdAt = reaction.createdAt
                        )
                    }
                }
            )
            is SocketEvent.MessagePinned -> messageDao.markPinned(
                messageId = event.messageId,
                pinnedBy = event.pinnedBy,
                pinnedAt = event.timestamp
            )
            is SocketEvent.MessageUnpinned -> messageDao.markUnpinned(event.messageId)
            is SocketEvent.ReadReceipt -> {
                val accountId = event.resolvedAccountId()
                if (accountId.isNotBlank() && conversationDao.exists(event.conversationId)) {
                    readReceiptDao.markRead(
                        conversationId = event.conversationId,
                        accountId = accountId,
                        lastReadMessageId = event.messageId,
                        handle = event.handle,
                        displayName = event.displayName,
                        avatarUrl = event.avatarUrl
                    )
                    if (accountId == secureStorage.getUserId()) {
                        conversationDao.markCurrentParticipantRead(event.conversationId, event.messageId)
                    }
                }
            }
            is SocketEvent.UserTyping -> handleTyping(event)
            is SocketEvent.UserStopTyping -> removeTypingUser(event.conversationId, event.accountId)
            is SocketEvent.PresenceStatus -> handlePresenceStatus(event)
            is SocketEvent.GroupInfoUpdated -> {
                if (!conversationDao.exists(event.conversationId)) {
                    syncConversationListFromServer()
                }
                conversationDao.updateGroupInfo(
                    conversationId = event.conversationId,
                    title = event.title,
                    avatarUrl = event.avatarUrl,
                    updatedAt = Instant.now().toString()
                )
            }
            is SocketEvent.GroupDisbanded -> {
                conversationDao.deleteConversation(event.conversationId)
                subscribeCurrentConversationRooms()
            }
            is SocketEvent.GroupMemberAdded -> syncConversationMembersOrList(event.conversationId)
            is SocketEvent.GroupMemberRemoved -> {
                if (event.removedUserId == secureStorage.getUserId()) {
                    conversationDao.deleteConversation(event.conversationId)
                    subscribeCurrentConversationRooms()
                } else {
                    syncConversationListFromServer()
                    syncConversationMembersOrList(event.conversationId)
                }
            }
            is SocketEvent.GroupRoleChanged -> syncConversationMembersOrList(event.conversationId)
            is SocketEvent.GroupYouAdded -> {
                syncConversationListFromServer()
                socketManager.joinRoom(event.conversationId)
                syncConversationMembersOrList(event.conversationId)
            }
            else -> Unit
        }
    }

    private suspend fun syncConversationMembersOrList(conversationId: String) {
        if (!conversationDao.exists(conversationId)) {
            syncConversationListFromServer()
        }
        if (conversationDao.exists(conversationId)) {
            syncConversationMembers(conversationId)
        }
    }

    private suspend fun syncConversationMembers(conversationId: String) {
        if (!conversationDao.exists(conversationId)) return
        when (val result = safeApiCall { conversationApi.getConversationMembers(conversationId) }) {
            is NetworkResult.Success -> {
                conversationDao.replaceParticipantPreviews(
                    conversationId = conversationId,
                    participantPreviews = result.data.toParticipantPreviewEntities(conversationId)
                )
            }
            is NetworkResult.Error -> Unit
        }
    }

    private suspend fun syncConversationListFromServer(
        preserveConversationIds: Set<String> = emptySet()
    ) {
        when (val result = safeApiCall { conversationApi.getConversations() }) {
            is NetworkResult.Success -> {
                val conversations = result.data.data
                val localById = conversationDao.getAllConversations()
                    .associateBy { it.conversation.id }
                val preservedIds = preserveConversationIds + activeConversationId.value
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::setOf)
                    .orEmpty()
                conversationDao.replaceAll(
                    conversations = conversations.map { it.toEntity() },
                    participantPreviews = conversations.flatMap { dto ->
                        dto.toParticipantPreviewEntities()
                            .withLocalIdentityFallback(localById[dto.id]?.participantPreviews)
                    },
                    preservedConversationIds = preservedIds
                )
                hydrateMissingDirectParticipantIdentities()
                subscribeCurrentConversationRooms()
            }
            is NetworkResult.Error -> Unit
        }
    }

    private suspend fun sendOptimisticMessage(
        conversationId: String,
        content: String,
        type: String,
        attachments: List<String>,
        clientTempId: String = UUID.randomUUID().toString()
    ): Result<Unit> {
        val senderId = secureStorage.getUserId()
            ?: return Result.failure(IllegalStateException("Current user is missing"))
        if (!conversationDao.exists(conversationId)) {
            return Result.failure(IllegalStateException("Conversation is not available"))
        }
        val graph = optimisticMessageGraph(
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            type = type,
            clientTempId = clientTempId,
            attachments = attachments
        )

        messageDao.upsertMessageGraphs(
            messages = listOf(graph.message),
            attachments = graph.attachments,
            reactions = graph.reactions
        )
        updateLastMessage(
            conversationId = conversationId,
            messageId = clientTempId,
            content = displayContent(content, type, isUnsent = false, attachments = attachments),
            senderId = senderId,
            timestamp = graph.message.createdAt,
            type = type,
            unreadIncrement = 0
        )

        return emitPendingMessage(
            conversationId = conversationId,
            content = content,
            type = type,
            clientTempId = clientTempId,
            attachments = attachments
        )
    }

    private suspend fun emitPendingMessage(
        conversationId: String,
        content: String,
        type: String,
        clientTempId: String,
        attachments: List<String>
    ): Result<Unit> {
        val payload = SendMessagePayload(
            conversationId = conversationId,
            content = content,
            type = type,
            clientTempId = clientTempId,
            attachments = attachments
        )

        val ackResult = socketManager.sendMessageWithAck(payload)
        return ackResult.fold(
            onSuccess = { ack ->
                val serverId = ack.messageId
                if (!serverId.isNullOrBlank()) {
                    messageDao.attachServerIdToClientTemp(clientTempId, serverId)
                }
                Result.success(Unit)
            },
            onFailure = { error ->
                messageDao.markSendFailed(clientTempId)
                Result.failure(error)
            }
        )
    }

    private suspend fun persistMessageDtos(dtos: List<MessageDto>) {
        val graphs = dtos.asReversed().map { it.toEntityGraph() }
        messageDao.upsertMessageGraphs(
            messages = graphs.map { it.message },
            attachments = graphs.flatMap { it.attachments },
            reactions = graphs.flatMap { it.reactions }
        )
    }

    private suspend fun updateConversationPreviewFromLatestMessage(
        conversationId: String,
        messages: List<MessageDto>
    ) {
        val latestMessage = messages.maxByOrNull { it.createdAt }
        if (latestMessage == null) {
            conversationDao.clearLastMessage(conversationId, updatedAt = null)
            return
        }
        val latestVisibleMessage = messages
            .filterNot { it.isDeleted }
            .maxByOrNull { it.createdAt }
        if (latestVisibleMessage == null) {
            conversationDao.clearLastMessage(conversationId, latestMessage.createdAt)
            return
        }
        updateLastMessage(
            conversationId = conversationId,
            messageId = latestVisibleMessage.id,
            content = displayContent(
                content = latestVisibleMessage.content.orEmpty(),
                type = latestVisibleMessage.type,
                isUnsent = latestVisibleMessage.isUnsent,
                attachments = latestVisibleMessage.attachments.orEmpty()
            ),
            senderId = latestVisibleMessage.senderId,
            senderName = latestVisibleMessage.previewSenderName(),
            timestamp = latestVisibleMessage.createdAt,
            type = latestVisibleMessage.type,
            unreadIncrement = 0
        )
    }

    private suspend fun handleReceiveMessage(event: SocketEvent.ReceiveMessage) {
        val payload = event.payload
        val conversationState = ensureConversationForIncomingMessage(payload)
        if (conversationState == IncomingConversationState.Missing) return

        val graph = payload.toEntityGraph()
        messageDao.upsertMessageGraphs(
            messages = listOf(graph.message),
            attachments = graph.attachments,
            reactions = graph.reactions
        )

        val currentAccountId = secureStorage.getUserId()
        val isOwnMessage = payload.isSentByCurrentAccount(currentAccountId)
        val isActiveConversation = activeConversationId.value == payload.conversationId
        val shouldUseServerUnreadCount = conversationState == IncomingConversationState.SyncedFromServer

        updateLastMessage(
            conversationId = payload.conversationId,
            messageId = payload.id,
            content = displayContent(
                content = payload.content.orEmpty(),
                type = payload.type,
                isUnsent = payload.isUnsent,
                attachments = payload.attachments.orEmpty()
            ),
            senderId = payload.senderId,
            senderName = graph.message.senderDisplayName ?: graph.message.senderHandle,
            timestamp = payload.createdAt,
            type = payload.type,
            unreadIncrement = if (!isOwnMessage && !isActiveConversation && !shouldUseServerUnreadCount) 1 else 0
        )

        if (!isOwnMessage && isActiveConversation) {
            markRead(payload.conversationId, payload.id)
        }
    }

    private suspend fun handleNewMessageNotification(event: SocketEvent.NewNotification) {
        if (!event.type.isNewMessageType()) return
        val candidateConversationIds = event.conversationReferenceIds()
        if (candidateConversationIds.isEmpty()) {
            syncConversationListFromServer()
            return
        }

        syncConversationListFromServer(preserveConversationIds = candidateConversationIds.toSet())
        val conversationId = candidateConversationIds.firstOrNull { conversationDao.exists(it) }
            ?: candidateConversationIds.first()
        if (!conversationDao.exists(conversationId)) {
            upsertNotificationConversationPlaceholder(event)
            subscribeCurrentConversationRooms()
        }
        if (conversationDao.exists(conversationId)) {
            syncLatestMessages(conversationId)
        }
    }

    private suspend fun ensureConversationForIncomingMessage(
        payload: MessagePayload
    ): IncomingConversationState {
        if (conversationDao.exists(payload.conversationId)) {
            return IncomingConversationState.Existing
        }

        syncConversationListFromServer(preserveConversationIds = setOf(payload.conversationId))
        if (conversationDao.exists(payload.conversationId)) {
            return IncomingConversationState.SyncedFromServer
        }

        upsertIncomingConversationPlaceholder(payload)
        return if (conversationDao.exists(payload.conversationId)) {
            IncomingConversationState.Placeholder
        } else {
            IncomingConversationState.Missing
        }
    }

    private suspend fun upsertIncomingConversationPlaceholder(payload: MessagePayload) {
        val currentAccountId = secureStorage.getUserId()
        val members = when (val result = safeApiCall {
            conversationApi.getConversationMembers(payload.conversationId)
        }) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> emptyList()
        }
        val currentMember = members.firstOrNull { it.normalizedAccountId() == currentAccountId }
        val otherMember = members.firstOrNull { it.normalizedAccountId() != currentAccountId }
        val isGroup = members.size > DIRECT_MEMBER_COUNT
        val senderName = payload.previewSenderName()
            ?: payload.previewSenderHandle()
            ?: "New message"
        val displayTitle = when {
            isGroup -> "Group conversation"
            otherMember != null -> otherMember.displayLabel()
            else -> senderName
        }
        val avatarUrl = if (isGroup) {
            null
        } else {
            otherMember?.avatarUrl ?: payload.previewSenderAvatarUrl()
        }

        conversationDao.upsertConversation(
            ConversationEntity(
                id = payload.conversationId,
                type = if (isGroup) "group" else "direct",
                displayTitle = displayTitle,
                avatarUrl = avatarUrl,
                isOnline = null,
                createdAt = payload.createdAt,
                updatedAt = payload.createdAt,
                unreadCount = 0,
                currentParticipantRole = currentMember?.role ?: "member",
                currentParticipantIsMuted = false,
                currentParticipantMutedUntil = null,
                currentParticipantLastReadMessageId = null,
                totalParticipants = members.size.takeIf { it > 0 } ?: DIRECT_MEMBER_COUNT,
                lastMessageId = payload.id,
                lastMessageContent = displayContent(
                    content = payload.content.orEmpty(),
                    type = payload.type,
                    isUnsent = payload.isUnsent,
                    attachments = payload.attachments.orEmpty()
                ),
                lastMessageSenderId = payload.senderId,
                lastMessageSenderName = senderName,
                lastMessageTimestamp = payload.createdAt,
                lastMessageType = payload.type
            )
        )

        val previews = if (members.isNotEmpty()) {
            members.toParticipantPreviewEntities(payload.conversationId)
        } else {
            listOf(payload.toSenderParticipantPreview(payload.conversationId))
        }
        conversationDao.replaceParticipantPreviews(payload.conversationId, previews)
        subscribeCurrentConversationRooms()
    }

    private suspend fun upsertNotificationConversationPlaceholder(event: SocketEvent.NewNotification) {
        val conversationId = event.conversationReferenceId() ?: return
        val actorName = event.actor?.displayLabel() ?: "New message"
        conversationDao.upsertConversation(
            ConversationEntity(
                id = conversationId,
                type = "direct",
                displayTitle = actorName,
                avatarUrl = event.actor?.avatarUrl,
                isOnline = null,
                createdAt = event.createdAt,
                updatedAt = event.createdAt,
                unreadCount = 1,
                currentParticipantRole = "member",
                currentParticipantIsMuted = false,
                currentParticipantMutedUntil = null,
                currentParticipantLastReadMessageId = null,
                totalParticipants = DIRECT_MEMBER_COUNT,
                lastMessageId = null,
                lastMessageContent = null,
                lastMessageSenderId = event.actor?.id,
                lastMessageSenderName = actorName,
                lastMessageTimestamp = event.createdAt,
                lastMessageType = "text"
            )
        )
        event.actor?.let { actor ->
            conversationDao.replaceParticipantPreviews(
                conversationId = conversationId,
                participantPreviews = listOf(
                    ConversationParticipantPreviewEntity(
                        conversationId = conversationId,
                        position = 0,
                        accountId = actor.id,
                        userId = actor.id,
                        handle = actor.handle ?: actor.username ?: actor.displayName ?: actor.id.orEmpty(),
                        displayName = actor.displayLabel() ?: actor.id.orEmpty(),
                        avatarUrl = actor.avatarUrl
                    )
                )
            )
        }
    }

    private suspend fun updateLastMessage(
        conversationId: String,
        messageId: String,
        content: String,
        senderId: String,
        senderName: String? = null,
        timestamp: String,
        type: String,
        unreadIncrement: Int
    ) {
        conversationDao.updateLastMessage(
            conversationId = conversationId,
            messageId = messageId,
            content = content,
            senderId = senderId,
            senderName = senderName,
            timestamp = timestamp,
            type = type,
            unreadIncrement = unreadIncrement
        )
    }

    private suspend fun refreshConversationPreviewAfterMessageMutation(
        conversationId: String,
        messageId: String
    ) {
        if (messageDao.getLocalId(messageId) == null) {
            syncLatestMessages(conversationId)
            return
        }
        refreshConversationPreviewFromLatestMessage(conversationId, syncFromServerIfEmpty = true)
    }

    private suspend fun refreshConversationPreviewFromLatestMessage(
        conversationId: String,
        syncFromServerIfEmpty: Boolean = false
    ) {
        val latestMessage = messageDao.getLatestVisibleMessage(conversationId)?.toDomain()
        if (latestMessage == null) {
            if (syncFromServerIfEmpty) {
                syncLatestMessages(conversationId)
                if (messageDao.getLatestVisibleMessage(conversationId) != null) return
            }
            conversationDao.clearLastMessage(conversationId, updatedAt = null)
            return
        }

        updateLastMessage(
            conversationId = conversationId,
            messageId = latestMessage.serverId ?: latestMessage.localId,
            content = displayContent(
                content = latestMessage.content,
                type = latestMessage.type,
                isUnsent = latestMessage.isUnsent,
                attachments = latestMessage.attachments.map { it.url }
            ),
            senderId = latestMessage.sender?.accountId ?: latestMessage.senderId,
            senderName = latestMessage.sender?.displayName ?: latestMessage.sender?.handle,
            timestamp = latestMessage.createdAt,
            type = latestMessage.type,
            unreadIncrement = 0
        )
    }

    private suspend fun handleTyping(event: SocketEvent.UserTyping) {
        val accountId = secureStorage.getUserId()
        if (event.accountId == accountId) return

        val typingUser = TypingUser(
            conversationId = event.conversationId,
            accountId = event.accountId,
            displayName = event.displayName
        )
        typingUsersByConversation.update { current ->
            val users = current[event.conversationId].orEmpty()
                .filterNot { it.accountId == event.accountId } + typingUser
            current + (event.conversationId to users)
        }

        val jobKey = "${event.conversationId}:${event.accountId}"
        typingExpiryJobs[jobKey]?.cancel()
        typingExpiryJobs[jobKey] = scope.launch {
            delay(TYPING_EXPIRY_MS)
            removeTypingUser(event.conversationId, event.accountId)
        }
    }

    private suspend fun handlePresenceStatus(event: SocketEvent.PresenceStatus) {
        val participantIds = listOfNotNull(
            event.accountId?.takeIf { it.isNotBlank() },
            event.userId?.takeIf { it.isNotBlank() }
        ).distinct()
        val conversationId = event.conversationId?.takeIf { it.isNotBlank() }
        if (participantIds.isEmpty() && conversationId == null) return

        val currentAccountId = secureStorage.getUserId()
        if (!currentAccountId.isNullOrBlank() && participantIds.contains(currentAccountId)) return

        val isOnline = event.isOnline ?: event.status.equals("online", ignoreCase = true)
        var updatedRows = 0
        if (conversationId != null) {
            updatedRows += conversationDao.updateDirectPresenceByConversationId(
                conversationId = conversationId,
                isOnline = isOnline
            )
        }
        if (participantIds.isNotEmpty()) {
            updatedRows += conversationDao.updateDirectPresence(
                participantIds = participantIds,
                isOnline = isOnline
            )
        }
        if (updatedRows == 0 && participantIds.isNotEmpty()) {
            hydrateMissingDirectParticipantIdentities()
            conversationDao.updateDirectPresence(
                participantIds = participantIds,
                isOnline = isOnline
            )
        }
    }

    private fun removeTypingUser(conversationId: String, accountId: String) {
        val jobKey = "$conversationId:$accountId"
        typingExpiryJobs.remove(jobKey)?.cancel()
        typingUsersByConversation.update { current ->
            val users = current[conversationId].orEmpty().filterNot { it.accountId == accountId }
            if (users.isEmpty()) current - conversationId else current + (conversationId to users)
        }
    }

    private fun displayContent(
        content: String,
        type: String,
        isUnsent: Boolean,
        attachments: List<String> = emptyList()
    ): String = displayMessagePreviewContent(content, type, isUnsent, attachments)

    private suspend fun hydrateMissingDirectParticipantIdentities() {
        conversationDao.getDirectConversations()
            .filter { it.needsDirectParticipantIdentityHydration() }
            .forEach { relation -> syncConversationMembers(relation.conversation.id) }
    }

    private suspend fun subscribeCurrentConversationRooms() {
        socketManager.subscribeConversationListRooms(
            conversationDao.getAllConversations().map { it.conversation.id }
        )
    }

    private fun ConversationWithParticipantPreviews.needsDirectParticipantIdentityHydration(): Boolean {
        if (!conversation.type.equals("direct", ignoreCase = true)) return false
        return participantPreviews.size < DIRECT_MEMBER_COUNT ||
            participantPreviews.any { it.accountId.isNullOrBlank() || it.userId.isNullOrBlank() }
    }

    private fun List<ConversationParticipantPreviewEntity>.withLocalIdentityFallback(
        local: List<ConversationParticipantPreviewEntity>?
    ): List<ConversationParticipantPreviewEntity> {
        val localPreviews = local.orEmpty()
        val localLooksHydrated = localPreviews.any {
            !it.accountId.isNullOrBlank() || !it.userId.isNullOrBlank()
        }
        if (!localLooksHydrated) return this
        if (isEmpty() || all { it.accountId.isNullOrBlank() && it.userId.isNullOrBlank() }) {
            return localPreviews
        }

        return map { preview ->
            if (!preview.accountId.isNullOrBlank() && !preview.userId.isNullOrBlank()) {
                preview
            } else {
                val localMatch = localPreviews.firstOrNull {
                    (!preview.accountId.isNullOrBlank() && it.accountId == preview.accountId) ||
                        (!preview.userId.isNullOrBlank() && it.userId == preview.userId) ||
                        it.handle == preview.handle ||
                        it.displayName == preview.displayName
                }
                preview.copy(
                    accountId = preview.accountId ?: localMatch?.accountId,
                    userId = preview.userId ?: localMatch?.userId
                )
            }
        }
    }

    private fun ConversationMemberDto.displayLabel(): String =
        displayName?.takeIf { it.isNotBlank() }
            ?: handle?.takeIf { it.isNotBlank() }
            ?: normalizedAccountId()

    private fun MessagePayload.senderAccountId(): String? =
        sender?.accountId
            ?: sender?.id
            ?: sender?.userId
            ?: senderId.takeIf { it.isNotBlank() }

    private fun MessagePayload.isSentByCurrentAccount(currentAccountId: String?): Boolean {
        val accountId = currentAccountId?.takeIf { it.isNotBlank() } ?: return false
        return senderId == accountId || senderAccountId() == accountId
    }

    private fun MessagePayload.previewSenderHandle(): String? =
        sender?.handle
            ?: sender?.username
            ?: senderHandle
            ?: handle

    private fun MessagePayload.previewSenderName(): String? =
        sender?.displayName
            ?: sender?.name
            ?: senderDisplayName
            ?: senderName
            ?: displayName
            ?: previewSenderHandle()

    private fun MessagePayload.previewSenderAvatarUrl(): String? =
        sender?.avatarUrl
            ?: sender?.avatar
            ?: senderAvatarUrl
            ?: senderAvatar
            ?: avatarUrl

    private fun MessagePayload.toSenderParticipantPreview(
        conversationId: String
    ): ConversationParticipantPreviewEntity {
        val accountId = senderAccountId()
        val handle = previewSenderHandle()
            ?: accountId
            ?: senderId
        return ConversationParticipantPreviewEntity(
            conversationId = conversationId,
            position = 0,
            accountId = accountId,
            userId = sender?.userId?.takeIf { it.isNotBlank() },
            handle = handle,
            displayName = previewSenderName() ?: handle,
            avatarUrl = previewSenderAvatarUrl()
        )
    }

    private fun MessageDto.previewSenderName(): String? =
        sender?.displayName
            ?: sender?.name
            ?: senderDisplayName
            ?: senderName
            ?: displayName
            ?: sender?.handle
            ?: sender?.username
            ?: senderHandle
            ?: handle

    private fun com.thuo_ng.swift_chat_android.core.socket.ReactionPayload.accountIds(): List<String> =
        (accountId?.let(::listOf) ?: userIds.orEmpty())
            .filter { it.isNotBlank() }

    private fun SocketEvent.ReadReceipt.resolvedAccountId(): String =
        accountId?.takeIf { it.isNotBlank() } ?: userId.orEmpty()

    private fun SocketEvent.NewNotification.conversationReferenceId(): String? =
        conversationReferenceIds().firstOrNull()

    private fun SocketEvent.NewNotification.conversationReferenceIds(): List<String> =
        listOfNotNull(
            payload?.conversationId?.takeIf { it.isNotBlank() },
            message?.conversationId?.takeIf { it.isNotBlank() },
            conversationId?.takeIf { it.isNotBlank() },
            referenceId?.takeIf { it.isNotBlank() },
            payload?.referenceId?.takeIf { it.isNotBlank() }
        ).distinct()

    private fun com.thuo_ng.swift_chat_android.core.socket.ActorPayload.displayLabel(): String? =
        displayName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: handle?.takeIf { it.isNotBlank() }

    private companion object {
        const val TYPING_EXPIRY_MS = 4_000L
        const val MESSAGE_PAGE_SIZE = 50
        const val DIRECT_MEMBER_COUNT = 2
    }

    private enum class IncomingConversationState {
        Existing,
        SyncedFromServer,
        Placeholder,
        Missing
    }
}
