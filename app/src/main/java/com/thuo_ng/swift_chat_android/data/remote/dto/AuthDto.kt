package com.thuo_ng.swift_chat_android.data.remote.dto

import com.google.gson.annotations.SerializedName

import com.thuo_ng.swift_chat_android.domain.model.AuthUser

// ── Request bodies ────────────────────────────────────────────────────────────

data class SignInRequest(
    val username: String,
    val password: String
)

data class SignupRequest(
    val username: String? = null,
    val email: String,
    val password: String
)

data class GoogleLoginRequest(
    val idToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class LogoutRequest(
    val refreshToken: String
)

// ── Response bodies ───────────────────────────────────────────────────────────

data class AccountBriefDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String
)

data class AuthResponseDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("account") val account: AccountBriefDto
)

data class RefreshTokenResponseDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("account") val account: AccountBriefDto
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun AuthResponseDto.toAuthUser(): AuthUser =
    AuthUser(id = account.id, email = account.email)