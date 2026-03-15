package com.synex.mirarecorder.util

import androidx.compose.ui.graphics.Color

fun colorHash(input: String): Color {
    var hash = 5381L
    for (byte in input.toByteArray(Charsets.UTF_8)) {
        hash = ((hash shl 5) + hash) + byte.toLong()
    }
    val hue = ((hash % 360 + 360) % 360).toFloat()
    return Color.hsl(hue, 0.6f, 0.45f)
}
