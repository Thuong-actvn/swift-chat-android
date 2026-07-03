package com.thuo_ng.swift_chat_android.data.mapper

import com.thuo_ng.swift_chat_android.core.util.toEpochMilli
import com.thuo_ng.swift_chat_android.data.local.entity.UserEntity
import com.thuo_ng.swift_chat_android.data.remote.dto.UserResponseDto
import com.thuo_ng.swift_chat_android.domain.model.User

fun UserResponseDto.toEntity(): UserEntity =
    UserEntity(
        id = id,
        username = username,
        handle = handle,
        displayName = displayName,
        email = email,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        bio = bio,
        website = website,
        location = location,
        lastSeen = lastSeen?.toEpochMilli(),
        createdAt = createdAt.toEpochMilli()
    )

fun UserEntity.toDomain(): User =
    User(
        id = id,
        username = username,
        handle = handle,
        displayName = displayName,
        email = email,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        bio = bio,
        website = website,
        location = location,
        lastSeen = lastSeen,
        createdAt = createdAt
    )
