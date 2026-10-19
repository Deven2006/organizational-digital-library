package com.example.digitallibrary.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.digitallibrary.data.model.Profile
import com.example.digitallibrary.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository internal constructor(
    private val context: Context?,
    private val prefs: SharedPreferences,
    private val supabaseClient: SupabaseClient = SupabaseClient()
) {
    constructor(context: Context, supabaseClient: SupabaseClient = SupabaseClient()) : this(
        context = context,
        prefs = context.getSharedPreferences("digital_library_auth", Context.MODE_PRIVATE),
        supabaseClient = supabaseClient
    )

    private val _isLoggedIn = MutableStateFlow(hasActiveSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getAuthToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        return if (token.isNullOrBlank()) null else token
    }

    /**
     * Checks if a JWT token is expired or close to expiring (within 60s).
     * Decodes the payload segment and parses the 'exp' claim, with fallback to KEY_EXPIRES_AT.
     */
    fun isTokenExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true

        // 1. Check cached expires_at timestamp if present
        val cachedExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()
        if (cachedExpiresAt > 0L) {
            return now >= (cachedExpiresAt - 60_000L)
        }

        // 2. Fallback: Parse exp claim directly from the JWT payload
        val parts = token.split(".")
        if (parts.size != 3) return true // Invalid JWT structure

        return try {
            val payloadBytes = try {
                java.util.Base64.getUrlDecoder().decode(parts[1])
            } catch (e: Throwable) {
                try {
                    android.util.Base64.decode(
                        parts[1],
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.DEFAULT
                    )
                } catch (e2: Throwable) {
                    null
                }
            } ?: return true

            val payloadJson = String(payloadBytes, Charsets.UTF_8)
            val expRegex = Regex(""""exp"\s*:\s*(\d+)""")
            val match = expRegex.find(payloadJson)
            if (match != null) {
                val expSeconds = match.groupValues[1].toLong()
                val expMillis = expSeconds * 1000L
                now >= (expMillis - 60_000L)
            } else {
                false
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Retrieves an active, validated access token.
     * Automatically attempts token refresh if the access token has expired or is nearing expiry.
     * If the session cannot be renewed, clears the invalid session to prevent repeated 401s.
     */
    suspend fun getValidAuthToken(): String? {
        val token = getAuthToken() ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expired = isTokenExpired(token)

        if (expired) {
            if (!refreshToken.isNullOrBlank()) {
                val refreshResult = refreshAuthToken()
                if (refreshResult.isSuccess) {
                    return refreshResult.getOrNull()
                } else {
                    // Refresh failed: session has been revoked or expired on server
                    logout()
                    return null
                }
            } else {
                // No refresh token available to renew expired token: clear dead session
                logout()
                return null
            }
        }
        return token
    }

    /**
     * Refreshes the session using the saved refresh token.
     */
    suspend fun refreshAuthToken(): Result<String> {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            ?: return Result.failure(IllegalStateException("No refresh token stored."))

        val refreshResult = supabaseClient.refreshToken(refreshToken)
        if (refreshResult.isSuccess) {
            val tokenResponse = refreshResult.getOrThrow()
            val now = System.currentTimeMillis()
            val expiresIn = tokenResponse.expiresIn ?: 3600L
            val expiresAt = now + (expiresIn * 1000L)

            val editor = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
                .putLong(KEY_EXPIRES_AT, expiresAt)
            if (!tokenResponse.refreshToken.isNullOrBlank()) {
                editor.putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            }
            editor.apply()
            _isLoggedIn.value = true
            return Result.success(tokenResponse.accessToken)
        } else {
            return Result.failure(refreshResult.exceptionOrNull() ?: Exception("Failed to refresh session."))
        }
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
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        if (token.isNullOrBlank() || userId.isNullOrBlank()) return false

        // If the access token is already expired and cannot be refreshed, the session is inactive
        if (isTokenExpired(token) && refreshToken.isNullOrBlank()) {
            return false
        }

        return true
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
        val refreshToken = tokenResponse.refreshToken
        val expiresIn = tokenResponse.expiresIn ?: 3600L
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

        // Fetch user profile from database
        val profileResult = supabaseClient.getProfile(userId, token)
        val profile = profileResult.getOrDefault(
            Profile(id = userId, fullName = userEmail.substringBefore("@"), role = "member")
        )

        // Save session locally to maintain logins on app restart
        val editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, userEmail)
            .putString(KEY_USER_ROLE, profile.role)
            .putString(KEY_USER_NAME, profile.fullName)
            .putLong(KEY_EXPIRES_AT, expiresAt)

        if (!refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()

        _isLoggedIn.value = true
        return Result.success(profile)
    }

    fun logout() {
        prefs.edit().clear().apply()
        // Purge cached private documents from disk upon logout
        try {
            val cacheFolder = context?.cacheDir?.let { java.io.File(it, "book_documents") }
            if (cacheFolder != null && cacheFolder.exists()) {
                cacheFolder.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isLoggedIn.value = false
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
    }
}
