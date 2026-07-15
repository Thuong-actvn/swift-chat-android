package com.thuo_ng.swift_chat_android.core.network

import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val secureStorage: SecureStorage,
    private val tokenRefreshManager: TokenRefreshManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse?.code == 401) return null

        return runBlocking {
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (!tokenRefreshManager.refreshAccessToken(requestToken)) {
                return@runBlocking null
            }

            val currentToken = secureStorage.getAccessToken() ?: return@runBlocking null
            response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }
    }
}
