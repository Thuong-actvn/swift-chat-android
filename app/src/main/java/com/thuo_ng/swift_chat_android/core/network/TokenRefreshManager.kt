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

    suspend fun establishSession(
        accessToken: String,
        refreshToken: String,
        userId: String
    ): Boolean = mutex.withLock {
        if (accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) {
            return@withLock false
        }

        secureStorage.saveSession(accessToken, refreshToken, userId)
        true
    }

    suspend fun logoutCurrentSession(
        revokeSession: suspend (refreshToken: String) -> Unit
    ) {
        val refreshToken = mutex.withLock {
            val refreshToken = secureStorage.getRefreshToken()

            secureStorage.clearAll()
            sessionManager.logout()

            refreshToken?.takeIf { it.isNotBlank() }
        }

        // Local logout is authoritative. Remote revocation is best-effort and uses the
        // refresh token captured after any in-flight refresh completed.
        if (refreshToken != null) {
            try {
                revokeSession(refreshToken)
            } catch (_: Exception) {
                // The device is already logged out locally; an offline revoke cannot block it.
            }
        }
    }

    suspend fun refreshAccessToken(requestToken: String? = null): Boolean {
        return mutex.withLock {
            val currentToken = secureStorage.getAccessToken()
            if (requestToken != null && currentToken == null) {
                // The session was cleared while this refresh request was waiting for the lock.
                return@withLock false
            }
            if (requestToken != null && currentToken != null && currentToken != requestToken) {
                return@withLock true
            }

            val currentRefreshToken = secureStorage.getRefreshToken()
            if (currentRefreshToken.isNullOrEmpty()) {
                secureStorage.clearAll()
                sessionManager.expireSession()
                return@withLock false
            }

            try {
                val response = authApiProvider.get().refreshToken(
                    RefreshTokenRequest(currentRefreshToken)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.accessToken.isBlank() ||
                            body.refreshToken.isBlank() ||
                            body.account.id.isBlank()
                        ) {
                            secureStorage.clearAll()
                            sessionManager.expireSession()
                            return@withLock false
                        }

                        secureStorage.saveSession(
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken,
                            userId = body.account.id
                        )
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
