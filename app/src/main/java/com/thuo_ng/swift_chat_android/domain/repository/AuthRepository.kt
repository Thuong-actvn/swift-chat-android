package com.thuo_ng.swift_chat_android.domain.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.data.remote.dto.AuthResponse
import com.thuo_ng.swift_chat_android.data.remote.dto.GoogleLoginRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignInRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignupRequest
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(request: SignInRequest): NetworkResult<AuthResponse>
    suspend fun signup(request: SignupRequest): NetworkResult<AuthResponse>
    suspend fun logout()

    suspend fun signInWithGoogle(request: GoogleLoginRequest): NetworkResult<AuthResponse>
    fun isUserLoggedIn(): Boolean

    val isLoggedInFlow: Flow<Boolean>
}
