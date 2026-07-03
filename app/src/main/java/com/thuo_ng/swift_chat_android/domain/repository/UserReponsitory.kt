package com.thuo_ng.swift_chat_android.domain.repository

import android.content.Context
import android.net.Uri
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateProfileDto
import com.thuo_ng.swift_chat_android.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser() : Flow<User?>
    suspend fun syncCurrentUser()
    suspend fun updateProfile(request: UpdateProfileDto): Result<User>
    suspend fun uploadFile(uri: Uri, context: Context): Result<String>
    suspend fun clearCurrentUser()
}