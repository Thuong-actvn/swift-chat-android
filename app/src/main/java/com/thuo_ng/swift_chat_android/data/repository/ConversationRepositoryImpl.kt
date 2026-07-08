package com.thuo_ng.swift_chat_android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.local.dao.ConversationDao
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.relation.ConversationWithParticipantPreviews
import com.thuo_ng.swift_chat_android.data.mapper.toDomain
import com.thuo_ng.swift_chat_android.data.mapper.toEntity
import com.thuo_ng.swift_chat_android.data.mapper.toParticipantPreviewEntities
import com.thuo_ng.swift_chat_android.data.remote.api.ConversationApi
import com.thuo_ng.swift_chat_android.data.remote.api.MessageApi
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationDetailDto
import com.thuo_ng.swift_chat_android.data.remote.dto.CreateConversationRequestDto
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
    private val conversationDao: ConversationDao,
    private val secureStorage: SecureStorage
) : ConversationRepository {

    private val gson = Gson()

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
                val localConversations = conversationDao.getAllConversations()
                val localById = localConversations.associateBy { it.conversation.id }
                val serverEntities = conversationDtos.map { it.toEntity() }

                conversationDao.replaceAll(
                    conversations = serverEntities,
                    participantPreviews = conversationDtos.flatMap { dto ->
                        dto.toParticipantPreviewEntities()
                            .withLocalAccountIdFallback(localById[dto.id]?.participantPreviews)
                    }
                )
                hydrateBlankAttachmentPreviews(serverEntities)
                NetworkResult.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun findDirectConversationWith(accountId: String): NetworkResult<Conversation?> {
        return when (val syncResult = syncConversations()) {
            is NetworkResult.Success -> {
                val conversation = hydrateDirectConversationMembersFor(accountId)
                    ?: conversationDao.getDirectConversations().findHydratedDirectConversationWith(accountId)
                    ?: syncResult.data.conversations.firstOrNull { it.isDirectConversationWith(accountId) }
                NetworkResult.Success(conversation)
            }
            is NetworkResult.Error -> {
                val localConversation = conversationDao.getDirectConversations()
                    .findHydratedDirectConversationWith(accountId)
                if (localConversation != null) {
                    NetworkResult.Success(localConversation)
                } else {
                    NetworkResult.Error(syncResult.code, syncResult.message)
                }
            }
        }
    }

    override suspend fun openOrCreateDirectConversation(accountId: String): NetworkResult<Conversation> {
        return createDirectConversation(accountId)
    }

    private suspend fun createDirectConversation(accountId: String): NetworkResult<Conversation> {
        val request = CreateConversationRequestDto(
            type = "direct",
            partnerId = accountId
        )

        return when (val result = safeApiCall { conversationApi.createConversation(request) }) {
            is NetworkResult.Success -> {
                val conversationDetail = parseConversationDetail(result.data)
                    ?: return NetworkResult.Error(message = "Could not open conversation")
                val currentAccountId = secureStorage.getUserId()

                conversationDao.upsertConversation(
                    conversationDetail.toEntity(
                        currentAccountId = currentAccountId,
                        partnerAccountId = accountId
                    )
                )
                conversationDao.replaceParticipantPreviews(
                    conversationId = conversationDetail.id,
                    participantPreviews = conversationDetail.toParticipantPreviewEntities()
                )
                NetworkResult.Success(
                    conversationDetail.toDomain(
                        currentAccountId = currentAccountId,
                        partnerAccountId = accountId
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    private fun parseConversationDetail(element: JsonElement): ConversationDetailDto? {
        return runCatching {
            val conversationElement = when {
                element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.get("data")
                element.isJsonObject && element.asJsonObject.has("conversation") -> element.asJsonObject.get("conversation")
                else -> element
            }
            gson.fromJson(conversationElement, ConversationDetailDto::class.java)
        }.getOrNull()
    }

    private suspend fun hydrateBlankAttachmentPreviews(conversations: List<ConversationEntity>) {
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

    private fun List<ConversationParticipantPreviewEntity>.withLocalAccountIdFallback(
        local: List<ConversationParticipantPreviewEntity>?
    ): List<ConversationParticipantPreviewEntity> {
        val localPreviews = local.orEmpty()
        val localLooksHydrated = localPreviews.size >= 2 &&
            localPreviews.any { !it.accountId.isNullOrBlank() }
        if (!localLooksHydrated) return this
        if (isEmpty() || all { it.accountId.isNullOrBlank() }) return localPreviews

        return map { preview ->
            if (!preview.accountId.isNullOrBlank()) {
                preview
            } else {
                val localMatch = localPreviews.firstOrNull {
                    it.handle == preview.handle ||
                        it.displayName == preview.displayName
                }
                preview.copy(accountId = localMatch?.accountId)
            }
        }
    }

    private suspend fun hydrateDirectConversationMembersFor(accountId: String): Conversation? {
        conversationDao.getDirectConversations()
            .filter { it.conversation.type.equals("direct", ignoreCase = true) }
            .forEach { relation ->
                when (val result = safeApiCall { conversationApi.getConversationMembers(relation.conversation.id) }) {
                    is NetworkResult.Success -> {
                        val participantPreviews = result.data.toParticipantPreviewEntities(relation.conversation.id)
                        conversationDao.replaceParticipantPreviews(
                            conversationId = relation.conversation.id,
                            participantPreviews = participantPreviews
                        )
                        if (participantPreviews.any { it.accountId == accountId }) {
                            return relation.copy(participantPreviews = participantPreviews).toDomain()
                        }
                    }
                    is NetworkResult.Error -> Unit
                }
            }
        return null
    }

    private fun List<ConversationWithParticipantPreviews>.findHydratedDirectConversationWith(
        accountId: String
    ): Conversation? {
        return firstOrNull { relation ->
            relation.conversation.type.equals("direct", ignoreCase = true) &&
                relation.hasHydratedParticipantPreviews() &&
                relation.participantPreviews.any { it.accountId == accountId }
        }?.toDomain()
    }

    private fun ConversationWithParticipantPreviews.hasHydratedParticipantPreviews(): Boolean {
        return participantPreviews.size >= 2 &&
            participantPreviews.any { !it.accountId.isNullOrBlank() }
    }

    private fun Conversation.isDirectConversationWith(accountId: String): Boolean {
        return type.equals("direct", ignoreCase = true) &&
            participantPreview.any { it.accountId == accountId }
    }
}
