package com.thuo_ng.swift_chat_android.ui.auth

// UI State

data class AuthUiState(
    // Fields nhập liệu
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,

    // Loading indicator
    val isLoading: Boolean = false,

    // Validation errors
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val usernameError: String? = null
) {
    val isSignInValid: Boolean
        get() = username.isNotBlank() && password.isNotBlank() &&
                usernameError == null && passwordError == null

    val isSignUpValid: Boolean
        get() = email.isNotBlank() && password.isNotBlank() &&
                confirmPassword.isNotBlank() && username.isNotBlank() &&
                emailError == null && passwordError == null &&
                confirmPasswordError == null && usernameError == null
}

// Intent
// Mọi hành động từ UI đều đi qua Intent → ViewModel.handleIntent().

sealed class AuthIntent {
    // Thay đổi giá trị field
    data class EmailChanged(val email: String) : AuthIntent()
    data class PasswordChanged(val password: String) : AuthIntent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthIntent()
    data class UsernameChanged(val username: String) : AuthIntent()

    // Toggle hiển thị password
    object PasswordVisibilityToggled : AuthIntent()
    object ConfirmPasswordVisibilityToggled : AuthIntent()

    // Submit form
    object SubmitSignIn : AuthIntent()
    object SubmitSignUp : AuthIntent()

    // Validate từng field khi mất focus
    object ValidateEmail : AuthIntent()
    object ValidatePassword : AuthIntent()
    object ValidateConfirmPassword : AuthIntent()
    object ValidateUsername : AuthIntent()
}

// Effect
// Side-effect một lần, phát qua Channel.

sealed class AuthEffect {
    object NavigateToMain : AuthEffect()
    object NavigateToSignIn : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
}
