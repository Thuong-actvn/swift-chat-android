package com.thuo_ng.swift_chat_android.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.session.SessionEvent
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.isUserLoggedIn()
        )

    val sessionEvent: SharedFlow<SessionEvent> = sessionManager.sessionEvent
}
