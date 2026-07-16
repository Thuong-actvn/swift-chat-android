package com.thuo_ng.swift_chat_android.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thuo_ng.swift_chat_android.core.session.SessionEvent
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.core.session.SessionRestoreResult
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import com.thuo_ng.swift_chat_android.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AppAuthState>(AppAuthState.Loading)
    val authState: StateFlow<AppAuthState> = _authState.asStateFlow()

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

        viewModelScope.launch {
            val restoreResult = authRepository.restoreSession()
            if (restoreResult == SessionRestoreResult.TemporaryFailure) {
                _effect.send(
                    AppEffect.ShowMessage(
                        "Unable to restore session. Please sign in again."
                    )
                )
            }

            // Start observing the token after startup restoration finishes.
            socketManager.start()
            authRepository.isLoggedInFlow.collect { isLoggedIn ->
                _authState.value = if (isLoggedIn) {
                    AppAuthState.Authenticated
                } else {
                    AppAuthState.Unauthenticated
                }
            }
        }
    }
}
