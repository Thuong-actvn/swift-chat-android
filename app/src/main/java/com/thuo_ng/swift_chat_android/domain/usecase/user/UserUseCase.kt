package com.thuo_ng.swift_chat_android.domain.usecase.user

import android.content.Context
import android.net.Uri
import com.thuo_ng.swift_chat_android.data.remote.dto.UpdateProfileDto
import com.thuo_ng.swift_chat_android.domain.model.User
import com.thuo_ng.swift_chat_android.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<User?> =
        userRepository.observeCurrentUser()
}

class SyncCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() =
        userRepository.syncCurrentUser()
}

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(request: UpdateProfileDto): Result<User> =
        userRepository.updateProfile(request)
}

class UploadFileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uri: Uri, context: Context): Result<String> =
        userRepository.uploadFile(uri, context)
}