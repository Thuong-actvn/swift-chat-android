package com.thuo_ng.swift_chat_android.ui.app

sealed interface AppAuthState {
    data object Loading : AppAuthState
    data object Authenticated : AppAuthState
    data object Unauthenticated : AppAuthState
}

sealed interface AppEffect {
    data class ShowMessage(val message: String) : AppEffect
}