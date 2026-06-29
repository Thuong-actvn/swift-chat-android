package com.thuo_ng.swift_chat_android.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.thuo_ng.swift_chat_android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface GoogleSignInHelper {
    suspend fun getIdToken(activityContext: Context): Result<String>
}

class GoogleSignInHelperImpl @Inject constructor(
    @param : ApplicationContext private val appContext: Context
) : GoogleSignInHelper {
    private val credentialManager = CredentialManager.create(appContext)
    override suspend fun getIdToken(activityContext: Context): Result<String> {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return runCatching {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    googleIdTokenCredential.idToken
                } else {
                    error("Unexpected credential type")
                }
        }
    }
}