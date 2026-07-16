package com.thuo_ng.swift_chat_android.domain.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.session.SessionRestoreResult
import com.thuo_ng.swift_chat_android.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(username: String, password: String): NetworkResult<AuthUser>
    suspend fun signup(username: String, email: String, password: String): NetworkResult<AuthUser>
    suspend fun signInWithGoogle(idToken: String): NetworkResult<AuthUser>
    suspend fun logout()
    val isLoggedInFlow: Flow<Boolean>
    suspend fun restoreSession(): SessionRestoreResult
}
