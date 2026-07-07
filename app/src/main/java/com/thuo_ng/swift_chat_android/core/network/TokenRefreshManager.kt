package com.thuo_ng.swift_chat_android.core.network

import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.remote.api.AuthApi
import com.thuo_ng.swift_chat_android.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenRefreshManager @Inject constructor(
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager,
    @param:Named("AuthApi") private val authApiProvider: Provider<AuthApi>
) {
    private val mutex = Mutex()

    suspend fun refreshAccessToken(requestToken: String? = null): Boolean {
        return mutex.withLock {
            val currentToken = secureStorage.getAccessToken()
            if (requestToken != null && currentToken != null && currentToken != requestToken) {
                return@withLock true
            }

            val refreshToken = secureStorage.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                secureStorage.clearAll()
                sessionManager.expireSession()
                return@withLock false
            }

            try {
                val response = authApiProvider.get().refreshToken(RefreshTokenRequest(refreshToken))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        secureStorage.saveTokens(body.accessToken, body.refreshToken)
                        secureStorage.saveUserId(body.account.id)
                        return@withLock true
                    }

                    secureStorage.clearAll()
                    sessionManager.expireSession()
                    return@withLock false
                }

                if (response.code() == 401 || response.code() == 403) {
                    secureStorage.clearAll()
                    sessionManager.expireSession()
                }
                false
            } catch (_: Exception) {
                false
            }
        }
    }
}
