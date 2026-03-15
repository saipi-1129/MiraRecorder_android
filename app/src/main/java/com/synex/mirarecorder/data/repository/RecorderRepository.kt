package com.synex.mirarecorder.data.repository

import com.synex.mirarecorder.data.model.AddTargetRequest
import com.synex.mirarecorder.data.model.Recording
import com.synex.mirarecorder.data.model.Target
import com.synex.mirarecorder.data.model.ToggleTargetRequest
import com.synex.mirarecorder.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderRepository @Inject constructor(
    private val apiService: ApiService,
) {

    suspend fun getTargets(): List<Target> {
        return apiService.getTargets().targets
    }

    suspend fun addTarget(userId: String): Target {
        return apiService.addTarget(AddTargetRequest(userId))
    }

    suspend fun toggleTarget(userId: String, enabled: Boolean) {
        apiService.toggleTarget(userId, ToggleTargetRequest(enabled))
    }

    suspend fun deleteTarget(userId: String) {
        apiService.deleteTarget(userId)
    }

    suspend fun getRecordings(): List<Recording> {
        return apiService.getRecordings()
    }
}
