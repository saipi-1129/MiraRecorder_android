package com.synex.mirarecorder.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Recording(
    @SerializedName("filename") val filename: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("duration") val duration: Double,
    @SerializedName("startTime") val startTime: Double,
    @SerializedName("size") val size: Long,
) {
    val startDate: Date get() = Date((startTime).toLong())
}
