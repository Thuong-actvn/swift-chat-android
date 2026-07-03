package com.thuo_ng.swift_chat_android.data.repository

import android.content.Context
import android.net.Uri
import com.thuo_ng.swift_chat_android.core.storage.SecureStorage
import com.thuo_ng.swift_chat_android.core.util.toMultipartBody
import com.thuo_ng.swift_chat_android.data.local.dao.UserDao
import com.thuo_ng.swift_chat_android.data.mapper.toDomain
import com.thuo_ng.swift_chat_android.data.mapper.toEntity
import com.thuo_ng.swift_chat_android.data.remote.api.UserApi
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateProfileDto
import com.thuo_ng.swift_chat_android.domain.model.User
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi,
    private val secureStorage: SecureStorage
): UserRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> {
        return secureStorage.userIdFlow
            .flatMapLatest { userId ->
                if (userId == null) flowOf(null)
                else userDao.observe(userId).map { it?.toDomain() }
            }
    }

    override suspend fun syncCurrentUser() {
        val dto = userApi.getCurrentUser()
        secureStorage.saveUserId(dto.id)  // lưu userId lần đầu
        userDao.upsert(dto.toEntity())
    }

    override suspend fun updateProfile(request: UpdateProfileDto): Result<User> {
        return try {
            val updatedDto = userApi.updateProfile(request)
            userDao.updateProfile(
                id = updatedDto.id,
                handle = updatedDto.handle,
                displayName = updatedDto.displayName,
                avatarUrl = updatedDto.avatarUrl,
                coverUrl = updatedDto.coverUrl,
                bio = updatedDto.bio,
                website = updatedDto.website,
                location = updatedDto.location
            )
            Result.success(updatedDto.toEntity().toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        uri: Uri,
        context: Context
    ): Result<String> {
        return try {
            val part = uri.toMultipartBody(context)
            val response = userApi.uploadFile(part)
            
            if (response.isSuccessful) {
                val url = response.body()?.url
                if (url != null) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("Upload successful but URL is null"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Upload failed: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCurrentUser() {
        val currentUserId = secureStorage.getUserId() ?: return
        userDao.deleteById(currentUserId)
    }

}