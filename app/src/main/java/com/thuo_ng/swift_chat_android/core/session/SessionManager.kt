package com.thuo_ng.swift_chat_android.core.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _sessionEvent = MutableSharedFlow<SessionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sessionEvent: SharedFlow<SessionEvent> = _sessionEvent.asSharedFlow()

    fun expireSession() {
        _sessionEvent.tryEmit(SessionEvent.Expired)
    }

    fun logout() {
        _sessionEvent.tryEmit(SessionEvent.LoggedOut)
    }
}
