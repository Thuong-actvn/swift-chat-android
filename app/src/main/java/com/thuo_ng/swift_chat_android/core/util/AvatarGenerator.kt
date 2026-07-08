package com.thuo_ng.swift_chat_android.core.util

import android.graphics.*
import android.text.TextPaint

object AvatarGenerator {
    private val COLORS = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A6F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
    )

    fun generate(name: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor(getColorForName(name))
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Text
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.WHITE
        textPaint.textSize = size / 2f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD

        val letter = if (name.isNotEmpty()) name.substring(0, 1).uppercase() else "?"
        val xPos = size / 2f
        val yPos = (size / 2f - (textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawText(letter, xPos, yPos, textPaint)

        return bitmap
    }

    private fun getColorForName(name: String): String {
        val hash = name.hashCode()
        val index = kotlin.math.abs(hash) % COLORS.size
        return COLORS[index]
    }
}
