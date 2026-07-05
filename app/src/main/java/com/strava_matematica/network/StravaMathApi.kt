package com.strava_matematica.network

import com.strava_matematica.model.CalibrationRequest
import com.strava_matematica.model.CalibrationResponse
import com.strava_matematica.model.ActivityResponse
import com.strava_matematica.model.DrillBatch
import com.strava_matematica.model.DrillFlushRequest
import com.strava_matematica.model.DrillFlushResult
import com.strava_matematica.model.IdentifyTopicRequest
import com.strava_matematica.model.IdentifyTopicResponse
import com.strava_matematica.model.PublicProfile
import com.strava_matematica.model.ReviewSuggestion
import com.strava_matematica.model.SkillProgressItem
import com.strava_matematica.model.SprintHistoryItem
import com.strava_matematica.model.WeeklyRanking
import com.strava_matematica.model.SessionStartRequest
import com.strava_matematica.model.SessionStartResponse
import com.strava_matematica.model.SubmitRequest
import com.strava_matematica.model.SubmitResult
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("api/student/{studentId}/calibrate")
    suspend fun calibrate(
        @Path("studentId") studentId: String,
        @Body body: CalibrationRequest,
    ): CalibrationResponse

    @POST("api/identify-topic")
    suspend fun identifyTopic(@Body body: IdentifyTopicRequest): IdentifyTopicResponse

    // ── Drill ─────────────────────────────────────────────────────────────────
    @GET("api/drill/arithmetic")
    suspend fun getDrillBatch(
        @Query("count") count: Int = 30,
        @Query("level") level: String = "basic",
    ): DrillBatch

    @POST("api/drill/flush")
    suspend fun flushDrill(@Body request: DrillFlushRequest): DrillFlushResult

    // ── Profile ───────────────────────────────────────────────────────────────
    @GET("api/profile/{slug}")
    suspend fun getProfile(@Path("slug") slug: String): PublicProfile

    // ── Ranking ───────────────────────────────────────────────────────────────
    @GET("api/ranking/weekly")
    suspend fun getWeeklyRanking(@Query("limit") limit: Int = 20): WeeklyRanking

    // ── Activity ──────────────────────────────────────────────────────────────
    @GET("api/student/{studentId}/review-suggestions")
    suspend fun getReviewSuggestions(@Path("studentId") studentId: String): List<ReviewSuggestion>

    @GET("api/student/{studentId}/skill-progress")
    suspend fun getSkillProgress(@Path("studentId") studentId: String): List<SkillProgressItem>

    @GET("api/student/{studentId}/activity")
    suspend fun getActivity(
        @Path("studentId") studentId: String,
        @Query("days") days: Int = 35,
    ): ActivityResponse

    @GET("api/student/{studentId}/sessions")
    suspend fun getSessionHistory(
        @Path("studentId") studentId: String,
        @Query("limit") limit: Int = 20,
    ): List<SprintHistoryItem>

    @GET("api/student/{studentId}/timeline")
    suspend fun getStudentTimeline(
        @Path("studentId") studentId: String,
        @Query("days") days: Int = 30,
        @Query("include_strokes") includeStrokes: Boolean = false,
    ): JsonObject
}
