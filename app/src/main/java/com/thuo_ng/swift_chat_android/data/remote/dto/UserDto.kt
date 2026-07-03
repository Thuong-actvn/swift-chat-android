package com.thuo_ng.swift_chat_android.data.remote.dto


import com.google.gson.annotations.SerializedName

data class UserResponseDto(
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("coverUrl")
    val coverUrl: String?,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("displayName")
    val displayName: String?,
    @SerializedName("email")
    val email: String,
    @SerializedName("handle")
    val handle: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("lastSeen")
    val lastSeen: String?,
    @SerializedName("location")
    val location: String?,
    @SerializedName("username")
    val username: String,
    @SerializedName("website")
    val website: String?
)

data class UpdateProfileDto(
    val handle: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val coverUrl: String?,
    val bio: String?,
    val website: String?,
    val location: String?
)

