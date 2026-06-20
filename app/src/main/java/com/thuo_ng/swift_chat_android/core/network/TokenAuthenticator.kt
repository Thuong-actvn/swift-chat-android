package com.thuo_ng.swift_chat_android.core.network

import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.data.remote.api.AuthApi
import com.thuo_ng.swift_chat_android.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager,
    @param:Named("AuthApi") private val authApiProvider: Provider<AuthApi>
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") != null && response.priorResponse != null) {
            val priorAuth = response.priorResponse?.request?.header("Authorization")
            if (priorAuth != null && priorAuth != response.request.header("Authorization")) {
                return null
            }
        }

        return runBlocking { // Tạo Blocking Coroutine -> Tạo scope để gọi suspend function (refreshToken)->giữ thread hiện tại đứng im, chờ hết block chạy hết
            mutex.withLock {
                val currentToken = secureStorage.getAccessToken()

                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                if (currentToken != null && currentToken != requestToken) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = secureStorage.getRefreshToken()
                if (refreshToken == null) {
                    secureStorage.clearAll()
                    sessionManager.expireSession()
                    return@runBlocking null
                }

                try {
                    val apiResponse = authApiProvider.get().refreshToken(RefreshTokenRequest(refreshToken))
                    if (apiResponse.isSuccessful) {
                        apiResponse.body()?.let { tokenResponse ->
                            secureStorage.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                            return@runBlocking response.request.newBuilder()
                                .header("Authorization", "Bearer ${tokenResponse.accessToken}")
                                .build()
                        }
                    }
                    if (apiResponse.code() == 401) {
                        secureStorage.clearAll()
                        sessionManager.expireSession()
                        return@runBlocking null
                    }
                } catch (e: Exception) {
                    // Handle network exception during refresh
                    return@runBlocking null
                }
                null
            }
        }
    }
}
