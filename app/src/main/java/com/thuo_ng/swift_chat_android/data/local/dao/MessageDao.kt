package com.thuo_ng.swift_chat_android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.thuo_ng.swift_chat_android.data.local.entity.MessageAttachmentEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageEntity
import com.thuo_ng.swift_chat_android.data.local.entity.MessageReactionEntity
import com.thuo_ng.swift_chat_android.data.local.relation.MessageWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Transaction
    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId AND isDeleted = 0
        ORDER BY createdAt ASC
        """
    )
    fun observeMessages(conversationId: String): Flow<List<MessageWithDetails>>

    @Query(
        """
        SELECT serverId FROM messages
        WHERE conversationId = :conversationId AND serverId IS NOT NULL
        ORDER BY createdAt ASC
        LIMIT 1
        """
    )
    suspend fun getOldestServerMessageId(conversationId: String): String?

    @Transaction
    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId AND isDeleted = 0
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestVisibleMessage(conversationId: String): MessageWithDetails?

    @Query(
        """
        SELECT localId FROM messages
        WHERE serverId = :messageId OR localId = :messageId OR clientTempId = :messageId
        LIMIT 1
        """
    )
    suspend fun getLocalId(messageId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<MessageAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReactions(reactions: List<MessageReactionEntity>)

    @Query("DELETE FROM message_attachments WHERE messageLocalId IN (:messageLocalIds)")
    suspend fun deleteAttachmentsForMessages(messageLocalIds: List<String>)

    @Query("DELETE FROM message_reactions WHERE messageLocalId IN (:messageLocalIds)")
    suspend fun deleteReactionsForMessages(messageLocalIds: List<String>)

    @Query("UPDATE messages SET serverId = :serverId, sendStatus = 'SENT' WHERE clientTempId = :clientTempId")
    suspend fun attachServerIdToClientTemp(clientTempId: String, serverId: String)

    @Query("UPDATE messages SET sendStatus = 'FAILED' WHERE clientTempId = :clientTempId")
    suspend fun markSendFailed(clientTempId: String)

    @Query("UPDATE messages SET sendStatus = :sendStatus WHERE localId = :localId OR clientTempId = :localId")
    suspend fun updateSendStatus(localId: String, sendStatus: String)

    @Query(
        """
        UPDATE messages SET
            content = '',
            isUnsent = 1,
            isEdited = 0,
            updatedAt = :updatedAt
        WHERE serverId = :messageId OR localId = :messageId
        """
    )
    suspend fun markUnsent(messageId: String, updatedAt: String?)

    @Query("UPDATE messages SET isDeleted = 1 WHERE serverId = :messageId OR localId = :messageId")
    suspend fun deleteForMe(messageId: String)

    @Query(
        """
        UPDATE messages SET
            content = :content,
            isEdited = 1,
            updatedAt = :updatedAt
        WHERE serverId = :messageId OR localId = :messageId
        """
    )
    suspend fun updateEditedMessage(messageId: String, content: String, updatedAt: String?)

    @Query(
        """
        UPDATE messages SET
            isPinned = 1,
            pinnedBy = :pinnedBy,
            pinnedAt = :pinnedAt
        WHERE serverId = :messageId OR localId = :messageId
        """
    )
    suspend fun markPinned(messageId: String, pinnedBy: String?, pinnedAt: String?)

    @Query(
        """
        UPDATE messages SET
            isPinned = 0,
            pinnedBy = NULL,
            pinnedAt = NULL
        WHERE serverId = :messageId OR localId = :messageId
        """
    )
    suspend fun markUnpinned(messageId: String)

    @Transaction
    suspend fun upsertMessageGraphs(
        messages: List<MessageEntity>,
        attachments: List<MessageAttachmentEntity>,
        reactions: List<MessageReactionEntity>
    ) {
        val localIds = messages.map { it.localId }
        if (localIds.isNotEmpty()) {
            deleteAttachmentsForMessages(localIds)
            deleteReactionsForMessages(localIds)
            upsertMessages(messages)
        }
        if (attachments.isNotEmpty()) {
            upsertAttachments(attachments)
        }
        if (reactions.isNotEmpty()) {
            upsertReactions(reactions)
        }
    }

    @Transaction
    suspend fun replaceReactions(messageId: String, reactions: List<MessageReactionEntity>) {
        val localId = getLocalId(messageId) ?: return
        deleteReactionsForMessages(listOf(localId))
        if (reactions.isNotEmpty()) {
            upsertReactions(reactions.map { it.copy(messageLocalId = localId) })
        }
    }

    @Transaction
    suspend fun replaceAttachments(messageId: String, attachments: List<MessageAttachmentEntity>) {
        val localId = getLocalId(messageId) ?: return
        deleteAttachmentsForMessages(listOf(localId))
        if (attachments.isNotEmpty()) {
            upsertAttachments(attachments.map { it.copy(messageLocalId = localId) })
        }
    }
}
