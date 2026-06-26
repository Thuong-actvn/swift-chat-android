package com.thuo_ng.swift_chat_android.domain.usecase.validation

import javax.inject.Inject

class ValidateEmailUseCase @Inject constructor() {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    operator fun invoke(email: String): String? = when {
        email.isBlank() -> "Email cannot be empty"
        !email.matches(emailRegex) -> "Invalid email format"
        else -> null
    }
}
