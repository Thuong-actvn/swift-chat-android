package com.thuo_ng.swift_chat_android.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

fun Uri.toMultipartBody(context: Context, fieldName: String = "file"): MultipartBody.Part {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(this) ?: "application/octet-stream"

    val (fileName, fileSize) = contentResolver.query(
        this, null, null, null, null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use Pair(null, -1L)

        val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            .takeIf { it != -1 }
            ?.let { cursor.getString(it) }

        val size = cursor.getColumnIndex(OpenableColumns.SIZE)
            .takeIf { it != -1 }
            ?.let { cursor.getLong(it) }
            ?: -1L

        Pair(name, size)
    } ?: Pair(null, -1L)

    val resolvedFileName = fileName ?: "upload_${System.currentTimeMillis()}"

    val requestBody = object : RequestBody() {
        override fun contentType() = mimeType.toMediaType()

        override fun contentLength() = fileSize

        override fun writeTo(sink: BufferedSink) {
            contentResolver.openInputStream(this@toMultipartBody)
                ?.use { input -> sink.writeAll(input.source()) }
                ?: throw IllegalStateException("Cannot read file from URI: ${this@toMultipartBody}")
        }
    }

    return MultipartBody.Part.createFormData(fieldName, resolvedFileName, requestBody)
}