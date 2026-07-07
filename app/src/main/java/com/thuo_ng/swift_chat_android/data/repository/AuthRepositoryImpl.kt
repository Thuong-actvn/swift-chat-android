package com.thuo_ng.swift_chat_android.data.repository

import com.thuo_ng.swift_chat_android.core.network.NetworkResult
import com.thuo_ng.swift_chat_android.core.network.TokenRefreshManager
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.core.network.safeApiCall
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.remote.api.AuthApi
import com.thuo_ng.swift_chat_android.data.remote.api.UserApi
import com.thuo_ng.swift_chat_android.data.remote.dto.GoogleLoginRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.LogoutRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignInRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.SignupRequest
import com.thuo_ng.swift_chat_android.data.remote.dto.toAuthUser
import com.thuo_ng.swift_chat_android.domain.model.AuthUser
import com.thuo_ng.swift_chat_android.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class AuthRepositoryImpl @Inject constructor(
    @param:Named("AuthApi") private val authApi: AuthApi,
    private val userApi: UserApi,
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager,
    private val tokenRefreshManager: TokenRefreshManager
) : AuthRepository {

    override val isLoggedInFlow: Flow<Boolean> = secureStorage.tokenFlow.map { it != null }

    override suspend fun signIn(username: String, password: String): NetworkResult<AuthUser> {
        val result = safeApiCall { authApi.signIn(SignInRequest(username, password)) }
        return when (result) {
            is NetworkResult.Success -> {
                secureStorage.saveTokens(
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken
                )
                secureStorage.saveUserId(result.data.account.id)
                NetworkResult.Success(result.data.toAuthUser())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun signup(username: String, email: String, password: String): NetworkResult<AuthUser> {
        val result = safeApiCall { authApi.signup(SignupRequest(username, email, password)) }
        return when (result) {
            is NetworkResult.Success -> {
                secureStorage.saveTokens(
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken
                )
                secureStorage.saveUserId(result.data.account.id)
                NetworkResult.Success(result.data.toAuthUser())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): NetworkResult<AuthUser> {
        val result = safeApiCall { authApi.googleLogin(GoogleLoginRequest(idToken)) }
        return when (result) {
            is NetworkResult.Success -> {
                secureStorage.saveTokens(
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken
                )
                secureStorage.saveUserId(result.data.account.id)
                NetworkResult.Success(result.data.toAuthUser())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
        }
    }

    override suspend fun logout() {
        val refreshToken = secureStorage.getRefreshToken()
        if (!refreshToken.isNullOrEmpty()) {
            safeApiCall { 
                userApi.logout(LogoutRequest(refreshToken))
            }
        }
        secureStorage.clearAll()
        sessionManager.logout()
    }

    override fun isUserLoggedIn(): Boolean {
        return !secureStorage.getAccessToken().isNullOrEmpty()
    }

    override suspend fun refreshToken(): NetworkResult<Unit> {
        return if (tokenRefreshManager.refreshAccessToken()) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(401, "Unable to refresh token")
        }
    }
}
