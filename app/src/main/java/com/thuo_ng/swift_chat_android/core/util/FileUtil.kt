package com.thuo_ng.swift_chat_android.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun Context.createTempPictureUri(
    providerAuthority: String = "${this.packageName}.fileProvider"
): Uri? {
    val imageFile = File(this.cacheDir, "images").apply { mkdirs() }
    val tempFile = File.createTempFile(
        "JPEG_${System.currentTimeMillis()}_",
        ".jpg",
        imageFile
    )
    return FileProvider.getUriForFile(this, providerAuthority, tempFile)
}
