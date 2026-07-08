package com.thuo_ng.swift_chat_android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.core.socket.SocketManager
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.local.dao.ConversationDao
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.relation.ConversationWithParticipantPreviews
import com.thuo_ng.swift_chat_android.data.mapper.toDomain
import com.thuo_ng.swift_chat_android.data.mapper.toEntity
import com.thuo_ng.swift_chat_android.data.mapper.toParticipantPreviewEntities
import com.thuo_ng.swift_chat_android.data.remote.dto.AddConversationMembersRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ChangeMemberRoleRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationActionResponseDto
import com.thuo_ng.swift_chat_android.data.remote.api.ConversationApi
import com.thuo_ng.swift_chat_android.data.remote.api.MessageApi
import com.thuo_ng.swift_chat_android.data.remote.dto.ConversationDetailDto
import com.thuo_ng.swift_chat_android.data.remote.dto.CreateConversationRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.MuteConversationRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.TransferLeadershipRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateGroupInfoRequestDto
import com.thuo_ng.swift_chat_android.data.remote.dto.displayMessagePreviewContent
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.ConversationMember
import com.thuo_ng.swift_chat_android.domain.model.ConversationPage
import com.thuo_ng.swift_chat_android.domain.model.MuteDuration
import com.thuo_ng.swift_chat_android.domain.repository.ConversationRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi,
    private val messageApi: MessageApi,
    private val conversationDao: ConversationDao,
    private val socketManager: SocketManager,
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

    override suspend fun syncConversations(
        preserveConversationIds: Set<String>
    ): NetworkResult<ConversationPage> {
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
                            .withLocalIdentityFallback(localById[dto.id]?.participantPreviews)
                    },
                    preservedConversationIds = preserveConversationIds
                )
                hydrateMissingDirectParticipantIdentities()
                subscribeCurrentConversationRooms()
                hydrateBlankAttachmentPreviews(serverEntities)
                NetworkResult.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun getConversationMembers(conversationId: String): NetworkResult<List<ConversationMember>> {
        return syncConversationMembers(conversationId)
    }

    override suspend fun updateGroupInfo(
        conversationId: String,
        title: String?,
        avatarUrl: String?
    ): NetworkResult<Unit> {
        return when (val result = safeApiCall {
            conversationApi.updateGroupInfo(
                conversationId = conversationId,
                request = UpdateGroupInfoRequestDto(title = title, avatarUrl = avatarUrl)
            )
        }) {
            is NetworkResult.Success -> {
                conversationDao.updateGroupInfo(
                    conversationId = conversationId,
                    title = title?.takeIf { it.isNotBlank() },
                    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
                    updatedAt = Instant.now().toString()
                )
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun deleteConversation(conversationId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { conversationApi.deleteConversation(conversationId) }) {
            is NetworkResult.Success -> result.data.asUnitResult("Could not delete conversation")
                .alsoOnSuccess { conversationDao.deleteConversation(conversationId) }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun addMembers(
        conversationId: String,
        userIds: List<String>
    ): NetworkResult<List<ConversationMember>> {
        val ids = userIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return getConversationMembers(conversationId)

        return when (val result = safeApiCall {
            conversationApi.addMembers(
                conversationId = conversationId,
                request = AddConversationMembersRequestDto(userIds = ids)
            )
        }) {
            is NetworkResult.Success -> result.data.asMembersRefreshResult(
                conversationId = conversationId,
                errorMessage = "Could not add members"
            )
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun leaveGroup(conversationId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { conversationApi.leaveGroup(conversationId) }) {
            is NetworkResult.Success -> result.data.asUnitResult("Could not leave group")
                .alsoOnSuccess { conversationDao.deleteConversation(conversationId) }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun kickMember(
        conversationId: String,
        accountId: String
    ): NetworkResult<List<ConversationMember>> {
        return when (val result = safeApiCall { conversationApi.kickMember(conversationId, accountId) }) {
            is NetworkResult.Success -> result.data.asMembersRefreshResult(
                conversationId = conversationId,
                errorMessage = "Could not remove member"
            )
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun changeMemberRole(
        conversationId: String,
        accountId: String,
        role: String
    ): NetworkResult<List<ConversationMember>> {
        return when (val result = safeApiCall {
            conversationApi.changeMemberRole(
                conversationId = conversationId,
                accountId = accountId,
                request = ChangeMemberRoleRequestDto(role = role)
            )
        }) {
            is NetworkResult.Success -> result.data.asMembersRefreshResult(
                conversationId = conversationId,
                errorMessage = "Could not update member role",
                refreshList = true
            )
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun transferLeadership(
        conversationId: String,
        newLeaderId: String
    ): NetworkResult<List<ConversationMember>> {
        return when (val result = safeApiCall {
            conversationApi.transferLeadership(
                conversationId = conversationId,
                request = TransferLeadershipRequestDto(newLeaderId = newLeaderId)
            )
        }) {
            is NetworkResult.Success -> result.data.asMembersRefreshResult(
                conversationId = conversationId,
                errorMessage = "Could not transfer leadership",
                refreshList = true
            )
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun muteConversation(
        conversationId: String,
        duration: MuteDuration
    ): NetworkResult<String?> {
        return when (val result = safeApiCall {
            conversationApi.muteConversation(
                conversationId = conversationId,
                request = MuteConversationRequestDto(duration = duration.apiValue)
            )
        }) {
            is NetworkResult.Success -> {
                val response = result.data
                if (!response.success) {
                    NetworkResult.Error(message = "Could not mute conversation")
                } else {
                    conversationDao.updateCurrentParticipantMute(
                        conversationId = conversationId,
                        isMuted = true,
                        mutedUntil = response.mutedUntil
                    )
                    NetworkResult.Success(response.mutedUntil)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun unmuteConversation(conversationId: String): NetworkResult<Unit> {
        return when (val result = safeApiCall { conversationApi.unmuteConversation(conversationId) }) {
            is NetworkResult.Success -> {
                if (!result.data.success) {
                    NetworkResult.Error(message = "Could not unmute conversation")
                } else {
                    conversationDao.updateCurrentParticipantMute(
                        conversationId = conversationId,
                        isMuted = false,
                        mutedUntil = null
                    )
                    NetworkResult.Success(Unit)
                }
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

    private suspend fun syncConversationMembers(
        conversationId: String
    ): NetworkResult<List<ConversationMember>> {
        return when (val result = safeApiCall { conversationApi.getConversationMembers(conversationId) }) {
            is NetworkResult.Success -> {
                val members = result.data.map { it.toDomain() }
                conversationDao.replaceParticipantPreviews(
                    conversationId = conversationId,
                    participantPreviews = result.data.toParticipantPreviewEntities(conversationId)
                )
                conversationDao.updateTotalParticipants(
                    conversationId = conversationId,
                    totalParticipants = members.size
                )
                NetworkResult.Success(members)
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    private suspend fun ConversationActionResponseDto.asMembersRefreshResult(
        conversationId: String,
        errorMessage: String,
        refreshList: Boolean = false
    ): NetworkResult<List<ConversationMember>> {
        if (!success) return NetworkResult.Error(message = errorMessage)
        if (refreshList) syncConversations(preserveConversationIds = setOf(conversationId))
        return syncConversationMembers(conversationId)
    }

    private fun ConversationActionResponseDto.asUnitResult(errorMessage: String): NetworkResult<Unit> {
        return if (success) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(message = errorMessage)
        }
    }

    private suspend fun NetworkResult<Unit>.alsoOnSuccess(
        block: suspend () -> Unit
    ): NetworkResult<Unit> {
        if (this is NetworkResult.Success) block()
        return this
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

    private suspend fun hydrateMissingDirectParticipantIdentities() {
        conversationDao.getDirectConversations()
            .filter { it.needsDirectParticipantIdentityHydration() }
            .forEach { relation ->
                when (val result = safeApiCall { conversationApi.getConversationMembers(relation.conversation.id) }) {
                    is NetworkResult.Success -> {
                        val participantPreviews = result.data.toParticipantPreviewEntities(relation.conversation.id)
                        conversationDao.replaceParticipantPreviews(
                            conversationId = relation.conversation.id,
                            participantPreviews = participantPreviews
                        )
                        conversationDao.updateTotalParticipants(
                            conversationId = relation.conversation.id,
                            totalParticipants = participantPreviews.size
                        )
                    }
                    is NetworkResult.Error -> Unit
                }
            }
    }

    private suspend fun subscribeCurrentConversationRooms() {
        socketManager.subscribeConversationListRooms(
            conversationDao.getAllConversations().map { it.conversation.id }
        )
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
                        if (participantPreviews.any { it.accountId == accountId || it.userId == accountId }) {
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
                relation.participantPreviews.any { it.accountId == accountId || it.userId == accountId }
        }?.toDomain()
    }

    private fun ConversationWithParticipantPreviews.hasHydratedParticipantPreviews(): Boolean {
        return participantPreviews.size >= 2 &&
            participantPreviews.any { !it.accountId.isNullOrBlank() || !it.userId.isNullOrBlank() }
    }

    private fun ConversationWithParticipantPreviews.needsDirectParticipantIdentityHydration(): Boolean {
        if (!conversation.type.equals("direct", ignoreCase = true)) return false
        return participantPreviews.size < DIRECT_MEMBER_COUNT ||
            participantPreviews.any { it.accountId.isNullOrBlank() || it.userId.isNullOrBlank() }
    }

    private fun Conversation.isDirectConversationWith(accountId: String): Boolean {
        return type.equals("direct", ignoreCase = true) &&
            participantPreview.any { it.accountId == accountId }
    }

    private companion object {
        const val DIRECT_MEMBER_COUNT = 2
    }
}
