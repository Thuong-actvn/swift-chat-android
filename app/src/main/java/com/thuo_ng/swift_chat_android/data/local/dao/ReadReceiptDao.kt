package com.thuo_ng.swift_chat_android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thuo_ng.swift_chat_android.data.local.entity.ReadReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadReceiptDao {
    @Query("SELECT * FROM read_receipts WHERE conversationId = :conversationId")
    fun observeReadReceipts(conversationId: String): Flow<List<ReadReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadReceipts(readReceipts: List<ReadReceiptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadReceipt(readReceipt: ReadReceiptEntity)

    @Query(
        """
        SELECT * FROM read_receipts
        WHERE conversationId = :conversationId AND accountId = :accountId
        LIMIT 1
        """
    )
    suspend fun getReadReceipt(conversationId: String, accountId: String): ReadReceiptEntity?

    suspend fun markRead(
        conversationId: String,
        accountId: String,
        lastReadMessageId: String,
        handle: String? = null,
        displayName: String? = null,
        avatarUrl: String? = null
    ) {
        val existing = getReadReceipt(conversationId, accountId)
        upsertReadReceipt(
            ReadReceiptEntity(
                conversationId = conversationId,
                accountId = accountId,
                lastReadMessageId = lastReadMessageId,
                handle = handle ?: existing?.handle,
                displayName = displayName ?: existing?.displayName,
                avatarUrl = avatarUrl ?: existing?.avatarUrl
            )
        )
    }
}
