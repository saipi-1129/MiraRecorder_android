package com.synex.mirarecorder.data.remote

import com.synex.mirarecorder.data.model.AddTargetRequest
import com.synex.mirarecorder.data.model.Recording
import com.synex.mirarecorder.data.model.Target
import com.synex.mirarecorder.data.model.TargetsResponse
import com.synex.mirarecorder.data.model.ToggleTargetRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("api/targets")
    suspend fun getTargets(): TargetsResponse

    @POST("api/targets")
    suspend fun addTarget(@Body request: AddTargetRequest): Target

    @POST("api/targets/{userId}/toggle")
    suspend fun toggleTarget(
        @Path("userId") userId: String,
        @Body request: ToggleTargetRequest,
    ): Response<Unit>

    @DELETE("api/targets/{userId}")
    suspend fun deleteTarget(@Path("userId") userId: String): Response<Unit>

    @GET("api/recordings")
    suspend fun getRecordings(): List<Recording>
}
