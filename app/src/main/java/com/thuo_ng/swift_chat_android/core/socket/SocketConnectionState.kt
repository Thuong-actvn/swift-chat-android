package com.thuo_ng.swift_chat_android.core.socket

sealed class SocketConnectionState {
    data object Disconnected : SocketConnectionState()
    data object Connecting : SocketConnectionState()
    data object Connected : SocketConnectionState()
    data object Reconnecting : SocketConnectionState()
    data class AuthError(val message: String? = null) : SocketConnectionState()
    data class NetworkError(val message: String? = null) : SocketConnectionState()
}
