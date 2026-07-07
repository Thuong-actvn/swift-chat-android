package com.thuo_ng.swift_chat_android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.ConversationParticipantPreviewEntity
import com.thuo_ng.swift_chat_android.data.local.relation.ConversationWithParticipantPreviews
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Transaction
    @Query(
        """
        SELECT * FROM conversations
        ORDER BY COALESCE(lastMessageTimestamp, updatedAt, createdAt) DESC
        """
    )
    fun observeAll(): Flow<List<ConversationWithParticipantPreviews>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun observeById(conversationId: String): Flow<ConversationWithParticipantPreviews?>

    @Query("SELECT EXISTS(SELECT 1 FROM conversations WHERE id = :conversationId)")
    suspend fun exists(conversationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParticipantPreviews(participantPreviews: List<ConversationParticipantPreviewEntity>)

    @Query("DELETE FROM conversation_participant_previews")
    suspend fun clearParticipantPreviews()

    @Query("DELETE FROM conversation_participant_previews WHERE conversationId = :conversationId")
    suspend fun clearParticipantPreviews(conversationId: String)

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query(
        """
        UPDATE conversations SET
            displayTitle = COALESCE(:title, displayTitle),
            avatarUrl = COALESCE(:avatarUrl, avatarUrl),
            updatedAt = :updatedAt
        WHERE id = :conversationId
        """
    )
    suspend fun updateGroupInfo(
        conversationId: String,
        title: String?,
        avatarUrl: String?,
        updatedAt: String
    )

    @Query(
        """
        UPDATE conversations SET
            lastMessageId = :messageId,
            lastMessageContent = :content,
            lastMessageSenderId = :senderId,
            lastMessageSenderName = :senderName,
            lastMessageTimestamp = :timestamp,
            lastMessageType = :type,
            updatedAt = :timestamp,
            unreadCount = unreadCount + :unreadIncrement
        WHERE id = :conversationId
        """
    )
    suspend fun updateLastMessage(
        conversationId: String,
        messageId: String,
        content: String,
        senderId: String,
        senderName: String?,
        timestamp: String,
        type: String,
        unreadIncrement: Int
    )

    @Query(
        """
        UPDATE conversations SET
            currentParticipantLastReadMessageId = :messageId,
            unreadCount = 0
        WHERE id = :conversationId
        """
    )
    suspend fun markCurrentParticipantRead(
        conversationId: String,
        messageId: String
    )

    @Transaction
    suspend fun replaceParticipantPreviews(
        conversationId: String,
        participantPreviews: List<ConversationParticipantPreviewEntity>
    ) {
        clearParticipantPreviews(conversationId)
        if (participantPreviews.isNotEmpty()) {
            upsertParticipantPreviews(participantPreviews)
        }
    }

    @Transaction
    suspend fun replaceAll(
        conversations: List<ConversationEntity>,
        participantPreviews: List<ConversationParticipantPreviewEntity>
    ) {
        clearParticipantPreviews()
        clearConversations()
        if (conversations.isNotEmpty()) {
            upsertConversations(conversations)
        }
        if (participantPreviews.isNotEmpty()) {
            upsertParticipantPreviews(participantPreviews)
        }
    }
}
