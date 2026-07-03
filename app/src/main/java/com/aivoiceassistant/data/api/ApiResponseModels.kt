package com.aivoiceassistant.data.api

// ── Gemini Models (existing) ────────────────────────────────────────────────
data class GeminiRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>
)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)
data class Part(val text: String)
data class GeminiResponse(val candidates: List<Candidate>)
data class Candidate(val content: Content)

// ── Auth Models ─────────────────────────────────────────────────────────────
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String = ""
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val user_id: Int = 0,
    val name: String = "",
    val email: String = "",
    val message: String = ""
)

// ── Health Records Models ────────────────────────────────────────────────────
data class HealthRecordRequest(
    val user_id: Int,
    val blood_pressure: Int,
    val blood_sugar: Int,
    val heart_rate: Int
)

data class HealthRecord(
    val record_id: Int,
    val blood_pressure: Int,
    val blood_sugar: Int,
    val heart_rate: Int,
    val bp_status: String,
    val sugar_status: String,
    val hr_status: String,
    val recorded_at: String
)

data class HealthRecordListResponse(
    val success: Boolean,
    val records: List<HealthRecord> = emptyList(),
    val message: String = ""
)

data class BasicResponse(
    val success: Boolean,
    val message: String = "",
    val record_id: Int = 0,
    val alert_id: Int = 0,
    val request_id: Int = 0,
    val session_id: Int = 0,
    val message_id: Int = 0
)

// ── Consult Models ───────────────────────────────────────────────────────────
data class CreateSessionRequest(
    val action: String = "create_session",
    val user_id: Int
)

data class SaveMessageRequest(
    val action: String = "save_message",
    val session_id: Int,
    val role: String,       // "user" or "model"
    val content: String
)

// ── SOS Models ───────────────────────────────────────────────────────────────
data class SosRequest(
    val user_id: Int,
    val emergency_type: String,
    val latitude: Double,
    val longitude: Double
)

// ── Ambulance Models ─────────────────────────────────────────────────────────
data class AmbulanceRequest(
    val user_id: Int,
    val patient_name: String,
    val contact_phone: String,
    val emergency_type: String,
    val latitude: Double,
    val longitude: Double,
    val sms_sent: Int,
    val whatsapp_sent: Int,
    val triggered_via: String   // "Button", "Voice", "PanicVolume"
)
