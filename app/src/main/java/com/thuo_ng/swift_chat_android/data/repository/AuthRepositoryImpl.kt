package com.thuo_ng.swift_chat_android.data.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.remote.api.AuthApi
import com.thuo_ng.swift_chat_android.data.remote.dto.AuthResponse
import com.thuo_ng.swift_chat_android.data.remote.dto.GoogleLoginRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignInRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.LogoutRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignupRequest
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class AuthRepositoryImpl @Inject constructor(
    @param:Named("AuthApi") private val authApi: AuthApi,
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager
) : AuthRepository {

    override val isLoggedInFlow: Flow<Boolean> = secureStorage.tokenFlow.map {it != null}

    override suspend fun signIn(request: SignInRequest): NetworkResult<AuthResponse> {
        val result = safeApiCall { authApi.signIn(request) }
        if (result is NetworkResult.Success) {
            secureStorage.saveTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken
            )
        }
        return result
    }

    override suspend fun signup(request: SignupRequest): NetworkResult<AuthResponse> {
        val result = safeApiCall { authApi.signup(request) }
        if (result is NetworkResult.Success) {
            secureStorage.saveTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken
            )
        }
        return result
    }

    override suspend fun signInWithGoogle(request: GoogleLoginRequest): NetworkResult<AuthResponse> {
        val result = safeApiCall { authApi.googleLogin(request) }
        if (result is NetworkResult.Success) {
            secureStorage.saveTokens(
                accessToken = result.data.accessToken,
                refreshToken = result.data.refreshToken
            )
        }
        return result
    }

    override suspend fun logout() {
        val refreshToken = secureStorage.getRefreshToken()
        if (!refreshToken.isNullOrEmpty()) {
            safeApiCall { authApi.logout(LogoutRequest(refreshToken)) }
        }
        secureStorage.clearAll()
        sessionManager.logout()
    }

    override fun isUserLoggedIn(): Boolean {
        return !secureStorage.getAccessToken().isNullOrEmpty()
    }
}
