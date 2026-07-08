package com.thuo_ng.swift_chat_android.domain.repository

import android.content.Context
import android.net.Uri
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.socket.SocketEvent
import com.thuo_ng.swift_chat_android.domain.model.Conversation
import com.thuo_ng.swift_chat_android.domain.model.Message
import com.thuo_ng.swift_chat_android.domain.model.ReadReceipt
import com.thuo_ng.swift_chat_android.domain.model.TypingUser
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeConversation(conversationId: String): Flow<Conversation?>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    fun observeReadReceipts(conversationId: String): Flow<List<ReadReceipt>>
    fun observeTypingUsers(conversationId: String): Flow<List<TypingUser>>

    suspend fun syncLatestMessages(conversationId: String): NetworkResult<Int>
    suspend fun loadOlderMessages(conversationId: String): NetworkResult<Boolean>
    suspend fun sendTextMessage(
        conversationId: String,
        content: String,
        clientTempId: String? = null
    ): Result<Unit>
    suspend fun sendAttachmentMessage(
        conversationId: String,
        uri: Uri,
        type: String,
        context: Context
    ): Result<Unit>
    suspend fun retryMessage(message: Message): Result<Unit>
    suspend fun editMessage(message: Message, content: String): Result<Unit>
    suspend fun unsendMessage(message: Message): Result<Unit>
    suspend fun deleteForMe(message: Message): Result<Unit>
    suspend fun toggleReaction(message: Message, emoji: String): Result<Unit>
    suspend fun pinMessage(message: Message): Result<Unit>
    suspend fun unpinMessage(message: Message): Result<Unit>
    suspend fun markRead(conversationId: String, messageId: String)
    fun joinRoom(conversationId: String)
    fun leaveRoom(conversationId: String)
    fun sendTyping(conversationId: String)
    fun sendStopTyping(conversationId: String)
    suspend fun handleSocketEvent(event: SocketEvent)
}
