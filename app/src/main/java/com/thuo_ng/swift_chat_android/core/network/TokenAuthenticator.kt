package com.thuo_ng.swift_chat_android.core.network

import com.thuo_ng.swift_chat_android.core.session.SessionManager
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
        if (response.priorResponse?.code == 401) return null

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
                        val tokenResponse = apiResponse.body()
                        if (tokenResponse != null) {
                            secureStorage.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                            return@runBlocking response.request.newBuilder()
                                .header("Authorization", "Bearer ${tokenResponse.accessToken}")
                                .build()
                        }
                        // Body null dù 2xx — không dùng được token, expire để tránh loop
                        secureStorage.clearAll()
                        sessionManager.expireSession()
                        return@runBlocking null
                    }

                    // 401/403: token thực sự hết hạn hoặc bị revoke → expire session
                    // 5xx/khác: lỗi phía server tạm thời
                    if (apiResponse.code() == 401 || apiResponse.code() == 403) {
                        secureStorage.clearAll()
                        sessionManager.expireSession()
                    }
                    return@runBlocking null

                } catch (e: Exception) {
                    // Lỗi mạng khi refresh → không expire, để user retry sau
                    return@runBlocking null
                }
                null
            }
        }
    }
}
