package com.thuo_ng.swift_chat_android.domain.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.ConversationMember
import com.thuo_ng.swift_chat_android.domain.model.ConversationPage
import com.thuo_ng.swift_chat_android.domain.model.MuteDuration
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeConversation(conversationId: String): Flow<Conversation?>
    suspend fun syncConversations(
        preserveConversationIds: Set<String> = emptySet()
    ): NetworkResult<ConversationPage>
    suspend fun getConversationMembers(conversationId: String): NetworkResult<List<ConversationMember>>
    suspend fun updateGroupInfo(conversationId: String, title: String?, avatarUrl: String?): NetworkResult<Unit>
    suspend fun deleteConversation(conversationId: String): NetworkResult<Unit>
    suspend fun addMembers(conversationId: String, userIds: List<String>): NetworkResult<List<ConversationMember>>
    suspend fun leaveGroup(conversationId: String): NetworkResult<Unit>
    suspend fun kickMember(conversationId: String, accountId: String): NetworkResult<List<ConversationMember>>
    suspend fun changeMemberRole(
        conversationId: String,
        accountId: String,
        role: String
    ): NetworkResult<List<ConversationMember>>
    suspend fun transferLeadership(conversationId: String, newLeaderId: String): NetworkResult<List<ConversationMember>>
    suspend fun muteConversation(conversationId: String, duration: MuteDuration): NetworkResult<String?>
    suspend fun unmuteConversation(conversationId: String): NetworkResult<Unit>
    suspend fun findDirectConversationWith(accountId: String): NetworkResult<Conversation?>
    suspend fun openOrCreateDirectConversation(accountId: String): NetworkResult<Conversation>
}
