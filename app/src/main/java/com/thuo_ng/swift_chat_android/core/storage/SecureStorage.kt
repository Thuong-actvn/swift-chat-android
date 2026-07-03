package com.thuo_ng.swift_chat_android.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    @Suppress("DEPRECATION")
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    @Suppress("DEPRECATION")
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "key_access_token"
        private const val KEY_REFRESH_TOKEN = "key_refresh_token"
        private const val KEY_USER_ID = "key_user_id"
    }

    private val _tokenFlow = MutableStateFlow(sharedPreferences.getString(KEY_ACCESS_TOKEN, null))
    val tokenFlow : StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _userIdFlow = MutableStateFlow(sharedPreferences.getString(KEY_USER_ID, null))
    val userIdFlow: StateFlow<String?> = _userIdFlow.asStateFlow()
    
    @Synchronized
    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        _tokenFlow.value = accessToken
    }

    @Synchronized
    fun saveUserId(userId: String) {
        sharedPreferences.edit {
            putString(KEY_USER_ID, userId)
        }
        _userIdFlow.value = userId
    }
    @Synchronized
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    @Synchronized
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    @Synchronized
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    @Synchronized
    fun clearAll() {
        sharedPreferences.edit { clear() }
        _tokenFlow.value = null
        _userIdFlow.value = null
    }
}
