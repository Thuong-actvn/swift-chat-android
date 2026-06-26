package com.thuo_ng.swift_chat_android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.data.remote.dto.SignInRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignupRequest
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.thuo_ng.swift_chat_android.domain.usecase.validation.ValidateEmailUseCase
import com.thuo_ng.swift_chat_android.domain.usecase.validation.ValidatePasswordUseCase
import com.thuo_ng.swift_chat_android.domain.usecase.validation.ValidateConfirmPasswordUseCase
import com.thuo_ng.swift_chat_android.domain.usecase.validation.ValidateUsernameUseCase

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateConfirmPassword: ValidateConfirmPasswordUseCase,
    private val validateUsername: ValidateUsernameUseCase
) : ViewModel() {

    // State: UI observe qua collectAsState()
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Effect: side-effect một lần
    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect: Flow<AuthEffect> = _effect.receiveAsFlow()

    // Intent dispatcher
    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            // Thay đổi field → cập nhật value + xoá error (để user sửa lại)
            is AuthIntent.EmailChanged -> {
                _uiState.update { it.copy(email = intent.email, emailError = null) }
            }
            is AuthIntent.PasswordChanged -> {
                _uiState.update { it.copy(password = intent.password, passwordError = null) }
            }
            is AuthIntent.ConfirmPasswordChanged -> {
                _uiState.update { it.copy(confirmPassword = intent.confirmPassword, confirmPasswordError = null) }
            }
            is AuthIntent.UsernameChanged -> {
                _uiState.update { it.copy(username = intent.username, usernameError = null) }
            }

            // Toggle hiển thị password
            is AuthIntent.PasswordVisibilityToggled -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is AuthIntent.ConfirmPasswordVisibilityToggled -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }

            // Validate từng field khi blur
            is AuthIntent.ValidateEmail -> {
                val error = validateEmail(_uiState.value.email)
                _uiState.update { it.copy(emailError = error) }
            }
            is AuthIntent.ValidatePassword -> {
                val error = validatePassword(_uiState.value.password)
                _uiState.update { it.copy(passwordError = error) }
            }
            is AuthIntent.ValidateConfirmPassword -> {
                val error = validateConfirmPassword(
                    _uiState.value.password, _uiState.value.confirmPassword
                )
                _uiState.update { it.copy(confirmPasswordError = error) }
            }
            is AuthIntent.ValidateUsername -> {
                val error = validateUsername(_uiState.value.username)
                _uiState.update { it.copy(usernameError = error) }
            }

            // Submit
            is AuthIntent.SubmitSignIn -> signIn()
            is AuthIntent.SubmitSignUp -> signUp()
        }
    }

    // Sign In
    private fun signIn() {
        val state = _uiState.value

        // Validate tất cả fields trước khi gửi request
        val usernameError = validateUsername(state.username)
        val passwordError = validatePassword(state.password)

        if (usernameError != null || passwordError != null) {
            _uiState.update { it.copy(usernameError = usernameError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = authRepository.signIn(SignInRequest(state.username, state.password))) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(AuthEffect.NavigateToMain)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(AuthEffect.ShowError(result.message))
                }
            }
        }
    }

    // Sign Up
    private fun signUp() {
        val state = _uiState.value

        // Validate tất cả fields
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password, isSignUp = true)
        val confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword)
        val usernameError = validateUsername(state.username)

        if (emailError != null || passwordError != null || confirmPasswordError != null || usernameError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    usernameError = usernameError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = authRepository.signup(SignupRequest(state.email, state.password, state.username))) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(AuthEffect.NavigateToMain)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(AuthEffect.ShowError(result.message))
                }
            }
        }
    }
}
