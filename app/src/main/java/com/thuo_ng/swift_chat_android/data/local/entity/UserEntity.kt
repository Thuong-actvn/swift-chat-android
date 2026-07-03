package com.thuo_ng.swift_chat_android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val handle: String,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
    val coverUrl: String?,
    val bio: String?,
    val website: String?,
    val location: String?,
    val lastSeen: Long?,
    val createdAt: Long
)

