package com.thuo_ng.swift_chat_android.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.session.SessionEvent
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val authState: StateFlow<AppAuthState> = authRepository.isLoggedInFlow
        .map { isLoggedIn ->
            if (isLoggedIn) AppAuthState.Authenticated else AppAuthState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppAuthState.Loading
        )
    private val _effect = Channel<AppEffect>(Channel.BUFFERED)
    val effect: Flow<AppEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessionEvent.collect { event ->
                when(event) {
                    SessionEvent.Expired ->
                        _effect.send(AppEffect.ShowMessage("Session expired. Please log in again."))
                    SessionEvent.LoggedOut -> Unit
                }
            }
        }
    }
}
