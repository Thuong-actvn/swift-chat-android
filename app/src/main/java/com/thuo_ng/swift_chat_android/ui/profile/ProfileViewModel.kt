package com.thuo_ng.swift_chat_android.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            // Lắng nghe dữ liệu trong Room Database
            launch {
                userRepository.observeCurrentUser().collect { user ->
                    _uiState.update { it.copy(user = user, isLoading = user == null) }
                }
            }
            
            // Fetch dữ liệu mới từ Server để đồng bộ xuống Room
            try {
                userRepository.syncCurrentUser()
            } catch (e: Exception) {
                e.printStackTrace()
                if (_uiState.value.user == null) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LogoutClicked -> logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.clearCurrentUser()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to clear cached user during logout", e)
            }
            authRepository.logout()
        }
    }
}
