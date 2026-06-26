package com.thuo_ng.swift_chat_android.core.session

sealed class SessionEvent {
    data object Expired : SessionEvent()
    data object LoggedOut : SessionEvent()
}
