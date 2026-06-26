package com.thuo_ng.swift_chat_android.domain.usecase.validation

import javax.inject.Inject

class ValidateUsernameUseCase @Inject constructor() {
    private val usernameRegex = Regex("^[a-zA-Z0-9_]{3,20}$")

    operator fun invoke(username: String): String? = when {
        username.isBlank() -> "Username cannot be empty"
        username.length < 3 -> "Username must be at least 3 characters"
        username.length > 20 -> "Username cannot exceed 20 characters"
        !username.matches(usernameRegex) -> "Only letters, numbers, and underscores are allowed"
        else -> null
    }
}
