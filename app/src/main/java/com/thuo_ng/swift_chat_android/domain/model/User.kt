package com.thuo_ng.swift_chat_android.domain.model

data class User(
    val id: String,
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
) {
    val displayedName: String
        get() = displayName ?: username
}