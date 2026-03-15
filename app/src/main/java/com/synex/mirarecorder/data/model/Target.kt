package com.synex.mirarecorder.data.model

import com.google.gson.annotations.SerializedName

data class Target(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("isRecording") val isRecording: Boolean,
    @SerializedName("isLiveRecording") val isLiveRecording: Boolean,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("addedAt") val addedAt: String? = null,
)

data class TargetsResponse(
    @SerializedName("targets") val targets: List<Target>,
)

data class AddTargetRequest(
    @SerializedName("userId") val userId: String,
)

data class ToggleTargetRequest(
    @SerializedName("enabled") val enabled: Boolean,
)
