package com.thuo_ng.swift_chat_android.domain.usecase.validation

import javax.inject.Inject

class ValidatePasswordUseCase @Inject constructor() {
    /**
     * [isSignUp] = true → áp dụng rule chặt hơn (min 8 ký tự, phải có chữ số).
     * SignIn chỉ cần không rỗng — server sẽ reject nếu sai.
     */
    operator fun invoke(password: String, isSignUp: Boolean = false): String? = when {
        password.isBlank() -> "Password cannot be empty"
        isSignUp && password.length < 8 -> "Password must be at least 8 characters"
        isSignUp && !password.any { it.isDigit() } -> "Password must contain at least 1 number"
        isSignUp && !password.any { it.isLetter() } -> "Password must contain at least 1 letter"
        else -> null
    }
}
