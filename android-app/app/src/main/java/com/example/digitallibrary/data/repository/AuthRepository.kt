package com.example.digitallibrary.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.digitallibrary.data.model.Profile
import com.example.digitallibrary.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val context: Context,
    private val supabaseClient: SupabaseClient = SupabaseClient()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("digital_library_auth", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(hasActiveSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getAuthToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "member") ?: "member"
    }

    fun getUserFullName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    private fun hasActiveSession(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val userId = prefs.getString(KEY_USER_ID, null)
        return !token.isNullOrBlank() && !userId.isNullOrBlank()
    }

    suspend fun login(email: String, pass: String): Result<Profile> {
        val authResult = supabaseClient.signInWithPassword(email, pass)
        if (authResult.isFailure) {
            return Result.failure(authResult.exceptionOrNull() ?: Exception("Login failed."))
        }

        val tokenResponse = authResult.getOrThrow()
        val token = tokenResponse.accessToken
        val userId = tokenResponse.user.id
        val userEmail = tokenResponse.user.email ?: email

        // Fetch user profile from database
        val profileResult = supabaseClient.getProfile(userId, token)
        val profile = profileResult.getOrDefault(
            Profile(id = userId, fullName = userEmail.substringBefore("@"), role = "member")
        )

        // Save session locally to maintain logins on app restart
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, userEmail)
            .putString(KEY_USER_ROLE, profile.role)
            .putString(KEY_USER_NAME, profile.fullName)
            .apply()

        _isLoggedIn.value = true
        return Result.success(profile)
    }

    fun logout() {
        prefs.edit().clear().apply()
        // Purge cached private documents from disk upon logout
        try {
            val cacheFolder = java.io.File(context.cacheDir, "book_documents")
            if (cacheFolder.exists()) {
                cacheFolder.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoggedIn.value = false
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
    }
}
