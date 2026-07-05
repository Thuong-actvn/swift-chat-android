package com.thuo_ng.swift_chat_android.core.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class ImagePickerActions(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit
)

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri) -> Unit
): ImagePickerActions {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onImageSelected) }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempCameraUri?.let(onImageSelected)
            }
        }
    )

    return ImagePickerActions(
        pickFromGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        takePhoto = {
            tempCameraUri = context.createTempPictureUri()
            tempCameraUri?.let { cameraLauncher.launch(it) }
        }
    )
}