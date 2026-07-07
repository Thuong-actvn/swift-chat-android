package com.thuo_ng.swift_chat_android.data.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.data.local.dao.ConversationDao
import com.thuo_ng.swift_chat_android.data.mapper.toDomain
import com.thuo_ng.swift_chat_android.data.mapper.toEntity
import com.thuo_ng.swift_chat_android.data.mapper.toParticipantPreviewEntities
import com.thuo_ng.swift_chat_android.data.remote.api.ConversationApi
import com.thuo_ng.swift_chat_android.data.remote.api.MessageApi
import com.thuo_ng.swift_chat_android.data.remote.dto.displayMessagePreviewContent
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.ConversationPage
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi,
    private val messageApi: MessageApi,
    private val conversationDao: ConversationDao
) : ConversationRepository {

    override fun observeConversations(): Flow<List<Conversation>> {
        return conversationDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeConversation(conversationId: String): Flow<Conversation?> {
        return conversationDao.observeById(conversationId).map { it?.toDomain() }
    }

    override suspend fun syncConversations(): NetworkResult<ConversationPage> {
        return when (val result = safeApiCall { conversationApi.getConversations() }) {
            is NetworkResult.Success -> {
                val conversationDtos = result.data.data
                conversationDao.replaceAll(
                    conversations = conversationDtos.map { it.toEntity() },
                    participantPreviews = conversationDtos.flatMap { it.toParticipantPreviewEntities() }
                )
                hydrateBlankAttachmentPreviews(conversationDtos.map { it.toEntity() })
                NetworkResult.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    private suspend fun hydrateBlankAttachmentPreviews(conversations: List<com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity>) {
        conversations
            .filter { it.lastMessageId != null && it.lastMessageContent.isNullOrBlank() }
            .forEach { conversation ->
                when (val result = safeApiCall { messageApi.getMessages(conversationId = conversation.id, limit = 1) }) {
                    is NetworkResult.Success -> {
                        val message = result.data.firstOrNull() ?: return@forEach
                        val preview = displayMessagePreviewContent(
                            content = message.content,
                            type = message.type,
                            isUnsent = message.isUnsent,
                            attachments = message.attachments
                        )
                        if (preview.isBlank()) return@forEach

                        conversationDao.updateLastMessage(
                            conversationId = conversation.id,
                            messageId = message.id,
                            content = preview,
                            senderId = message.senderId,
                            senderName = conversation.lastMessageSenderName,
                            timestamp = message.createdAt,
                            type = message.type,
                            unreadIncrement = 0
                        )
                    }
                    is NetworkResult.Error -> Unit
                }
            }
    }
}
