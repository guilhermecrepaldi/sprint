package com.strava_matematica.network

import com.strava_matematica.model.SessionStartRequest
import com.strava_matematica.model.SessionStartResponse
import com.strava_matematica.model.SubmitRequest
import com.strava_matematica.model.SubmitResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StravaMathApi {
    @GET("api/health")
    suspend fun health(): Map<String, String>

    @POST("api/session/start")
    suspend fun startSession(@Body request: SessionStartRequest): SessionStartResponse

    @POST("api/session/{sessionId}/submit")
    suspend fun submitFolha(
        @Path("sessionId") sessionId: String,
        @Body request: SubmitRequest,
    ): SubmitResult
}
