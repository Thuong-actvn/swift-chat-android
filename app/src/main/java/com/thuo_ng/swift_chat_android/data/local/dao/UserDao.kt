package com.thuo_ng.swift_chat_android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thuo_ng.swift_chat_android.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE id = :id")
    fun observe(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("""
        UPDATE users SET
            handle = :handle,
            displayName = :displayName,
            avatarUrl = :avatarUrl,
            coverUrl = :coverUrl,
            bio = :bio,
            website = :website,
            location = :location
        WHERE id = :id
    """)
    suspend fun updateProfile(
        id: String,
        handle: String?,
        displayName: String?,
        avatarUrl: String?,
        coverUrl: String?,
        bio: String?,
        website: String?,
        location: String?
    )

    @Query("UPDATE users SET lastSeen = :lastSeen WHERE id = :id")
    suspend fun updatePresence(id: String, lastSeen: Long?)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)
}