package com.thuo_ng.swift_chat_android.domain.usecase.validation

import javax.inject.Inject

class ValidateConfirmPasswordUseCase @Inject constructor() {
    operator fun invoke(password: String, confirmPassword: String): String? = when {
        confirmPassword.isBlank() -> "Please confirm your password"
        confirmPassword != password -> "Passwords do not match"
        else -> null
    }
}
