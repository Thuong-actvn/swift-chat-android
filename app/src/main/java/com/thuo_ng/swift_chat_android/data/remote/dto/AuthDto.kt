package com.thuo_ng.swift_chat_android.data.remote.dto

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserBrief
)

data class UserBrief(
    val id: String,
    val email: String
)

data class LogoutRequest(
    val refreshToken: String
)

data class SignInRequest(
    val username: String,
    val password: String
)

data class SignupRequest(
    val email: String,
    val password: String,
    val username: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserBrief
)
