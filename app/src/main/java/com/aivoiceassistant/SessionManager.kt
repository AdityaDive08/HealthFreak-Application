package com.aivoiceassistant

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton session manager.
 * Stores the logged-in user's ID and name in SharedPreferences
 * so every activity can access it without re-logging in.
 *
 * Usage:
 *   SessionManager.init(applicationContext)  ← call once in LoginActivity
 *   SessionManager.userId                    ← read from any activity
 */
object SessionManager {

    private const val PREF_NAME    = "ai_voice_session"
    private const val KEY_USER_ID  = "user_id"
    private const val KEY_NAME     = "user_name"
    private const val KEY_EMAIL    = "user_email"
    private const val KEY_LOGGED   = "is_logged_in"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(userId: Int, name: String, email: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_LOGGED, true)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED, false)

    val userId: Int
        get() = prefs.getInt(KEY_USER_ID, 0)

    val userName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""

    val userEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""

    fun saveProfilePicture(base64Image: String) {
        val uid = userId
        if (uid > 0) {
            prefs.edit().putString("dp_$uid", base64Image).apply()
        }
    }

    fun getProfilePicture(): String? {
        val uid = userId
        if (uid > 0) {
            return prefs.getString("dp_$uid", null)
        }
        return null
    }
}
