package com.aivoiceassistant.data.api

import retrofit2.http.*

interface ApiService {
    // ── Gemini AI (existing) ─────────────────────────────────────────────────
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun sendPrompt(@Body request: GeminiRequest): GeminiResponse
}

interface PhpApiService {
    // ── Auth ─────────────────────────────────────────────────────────────────
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // ── Health Records ────────────────────────────────────────────────────────
    @POST("health_records")
    suspend fun saveHealthRecord(@Body request: HealthRecordRequest): BasicResponse

    @GET("health_records")
    suspend fun getHealthRecords(@Query("user_id") userId: Int): HealthRecordListResponse

    // ── Consult ───────────────────────────────────────────────────────────────
    @POST("consult")
    suspend fun createConsultSession(@Body request: CreateSessionRequest): BasicResponse

    @POST("consult")
    suspend fun saveConsultMessage(@Body request: SaveMessageRequest): BasicResponse

    // ── SOS ───────────────────────────────────────────────────────────────────
    @POST("sos_alert")
    suspend fun logSosAlert(@Body request: SosRequest): BasicResponse

    // ── Ambulance ─────────────────────────────────────────────────────────────
    @POST("ambulance")
    suspend fun logAmbulanceRequest(@Body request: AmbulanceRequest): BasicResponse
}
