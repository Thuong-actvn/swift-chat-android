package com.thuo_ng.swift_chat_android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thuo_ng.swift_chat_android.data.local.entity.NotificationEntity
import com.thuo_ng.swift_chat_android.data.local.entity.NotificationSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NotificationEntity?>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeLocalUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getLocalUnreadCount(): Int

    @Query("SELECT unreadCount FROM notification_summary WHERE id = 'singleton' LIMIT 1")
    fun observeStoredUnreadCount(): Flow<Int?>

    @Query("SELECT unreadCount FROM notification_summary WHERE id = 'singleton' LIMIT 1")
    suspend fun getStoredUnreadCount(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: NotificationSummaryEntity)

    suspend fun setUnreadCount(unreadCount: Int) {
        upsertSummary(NotificationSummaryEntity(unreadCount = unreadCount.coerceAtLeast(0)))
    }

    suspend fun incrementUnreadCount() {
        val storedCount = getStoredUnreadCount()
        setUnreadCount(storedCount?.plus(1) ?: getLocalUnreadCount())
    }

    suspend fun decrementUnreadCount() {
        val storedCount = getStoredUnreadCount()
        setUnreadCount(storedCount?.minus(1)?.coerceAtLeast(0) ?: getLocalUnreadCount())
    }

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    suspend fun replaceAll(notifications: List<NotificationEntity>) {
        clearAll()
        if (notifications.isNotEmpty()) {
            upsertAll(notifications)
        }
    }
}
