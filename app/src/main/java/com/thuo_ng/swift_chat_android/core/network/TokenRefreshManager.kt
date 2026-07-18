package com.thuo_ng.swift_chat_android.core.network

import android.util.Base64
import com.thuo_ng.swift_chat_android.core.session.SessionManager
import com.thuo_ng.swift_chat_android.core.session.SessionRestoreResult
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.remote.api.AuthApi
import com.thuo_ng.swift_chat_android.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
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
    private companion object {
        const val LOGOUT_TIMEOUT_MS = 5_000L
    }

    private enum class RefreshResult {
        Success,
        InvalidSession,
        TemporaryFailure
    }

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
        revokeSession: suspend (accessToken: String, refreshToken: String) -> Unit
    ) {
        mutex.withLock {
            val accessToken = secureStorage.getAccessToken()
            val refreshToken = secureStorage.getRefreshToken()

            try {
                if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
                    withTimeoutOrNull(LOGOUT_TIMEOUT_MS) {
                        revokeSession(accessToken, refreshToken)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Remote logout is best-effort; local logout still happens below.
            } finally {
                secureStorage.clearAll()
                sessionManager.logout()
            }
        }
    }

    suspend fun restoreSession(): SessionRestoreResult {
        val currentToken = secureStorage.getAccessToken()
        if (currentToken.isNullOrBlank()) {
            return SessionRestoreResult.Unauthenticated
        }
        if (!isAccessTokenExpired(currentToken)) {
            return SessionRestoreResult.Authenticated
        }

        return when (refreshAccessTokenResult(currentToken)) {
            RefreshResult.Success -> SessionRestoreResult.Authenticated
            RefreshResult.InvalidSession -> SessionRestoreResult.Unauthenticated
            RefreshResult.TemporaryFailure -> {
                // There is no offline/retry flow during startup yet. Remove the expired
                // local session so it cannot be exposed as authenticated.
                secureStorage.clearAll()
                SessionRestoreResult.TemporaryFailure
            }
        }
    }

    suspend fun refreshAccessToken(requestToken: String? = null): Boolean {
        return refreshAccessTokenResult(requestToken) == RefreshResult.Success
    }

    private suspend fun refreshAccessTokenResult(requestToken: String?): RefreshResult {
        return mutex.withLock {
            val currentToken = secureStorage.getAccessToken()
            if (requestToken != null && currentToken == null) {
                // The session was cleared while this refresh request was waiting for the lock.
                return@withLock RefreshResult.InvalidSession
            }
            if (requestToken != null && currentToken != null && currentToken != requestToken) {
                return@withLock RefreshResult.Success
            }

            val currentRefreshToken = secureStorage.getRefreshToken()
            if (currentRefreshToken.isNullOrEmpty()) {
                secureStorage.clearAll()
                sessionManager.expireSession()
                return@withLock RefreshResult.InvalidSession
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
                            return@withLock RefreshResult.InvalidSession
                        }

                        secureStorage.saveSession(
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken,
                            userId = body.account.id
                        )
                        return@withLock RefreshResult.Success
                    }

                    secureStorage.clearAll()
                    sessionManager.expireSession()
                    return@withLock RefreshResult.InvalidSession
                }

                if (response.code() == 401 || response.code() == 403) {
                    secureStorage.clearAll()
                    sessionManager.expireSession()
                    return@withLock RefreshResult.InvalidSession
                }
                RefreshResult.TemporaryFailure
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                // Network unavailable — the session is kept intact.
                // TokenAuthenticator will retry the refresh on the next API call.
                RefreshResult.TemporaryFailure
            } catch (_: Exception) {
                RefreshResult.TemporaryFailure
            }
        }
    }

    private fun isAccessTokenExpired(accessToken: String): Boolean {
        return try {
            val payload = accessToken.split('.').getOrNull(1) ?: return true
            val decodedPayload = Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            ).toString(Charsets.UTF_8)
            val expiresAt = JSONObject(decodedPayload).optLong("exp", -1L)
            if (expiresAt <= 0L) return true

            expiresAt <= System.currentTimeMillis() / 1_000L
        } catch (_: Exception) {
            // Unknown token format cannot be considered safely reusable.
            true
        }
    }
}
