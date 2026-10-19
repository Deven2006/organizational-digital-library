package com.example.digitallibrary

import com.example.digitallibrary.data.remote.SupabaseClient
import com.example.digitallibrary.data.remote.SupabaseConfig
import com.example.digitallibrary.data.repository.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class SupabaseAuthAndErrorTest {

    private val supabaseClient = SupabaseClient()

    @Test
    fun testSupabaseConfigIsValid() {
        assertTrue("SupabaseConfig should report configured", SupabaseConfig.isConfigured())
        assertTrue("Supabase URL must start with https://", SupabaseConfig.SUPABASE_URL.startsWith("https://"))
        assertFalse("Supabase URL must not contain placeholder", SupabaseConfig.SUPABASE_URL.contains("your-project-id"))
        assertTrue("Anon key must be populated", SupabaseConfig.SUPABASE_ANON_KEY.isNotBlank())
        assertFalse("Anon key must not contain placeholder", SupabaseConfig.SUPABASE_ANON_KEY.contains("your-anon-public-key"))
    }

    @Test
    fun testTokenSanitization() {
        val raw = "  Bearer my_token_123   "
        val sanitized = supabaseClient.sanitizeAuthToken(raw)
        assertEquals("my_token_123", sanitized)

        val alreadyClean = "clean_jwt_xyz"
        assertEquals("clean_jwt_xyz", supabaseClient.sanitizeAuthToken(alreadyClean))
    }

    @Test
    fun testTokenValidationRejectsBlankAndWhitespace() {
        val blankResult = supabaseClient.validateAuthToken("   ")
        assertTrue(blankResult.isFailure)
        assertTrue(blankResult.exceptionOrNull() is IllegalArgumentException)
        assertTrue(blankResult.exceptionOrNull()?.message?.contains("blank or empty") == true)

        val emptyBearer = supabaseClient.validateAuthToken("Bearer    ")
        assertTrue(emptyBearer.isFailure)
    }

    @Test
    fun testTokenValidationRejectsAnonKeyAsBearerJwt() {
        val result = supabaseClient.validateAuthToken(SupabaseConfig.SUPABASE_ANON_KEY)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be used as user Authorization Bearer") == true)
    }

    @Test
    fun testTokenValidationAcceptsValidUserToken() {
        val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakSignatures"
        val result = supabaseClient.validateAuthToken(validToken)
        assertTrue(result.isSuccess)
        assertEquals(validToken, result.getOrNull())
    }

    @Test
    fun testExtractErrorMessageFromPostgrest401() {
        val postgrestJson = """{"code":"PGRST301","details":null,"hint":null,"message":"Empty JWT is sent in Authorization header"}"""
        val errorMsg = supabaseClient.extractErrorMessage(postgrestJson, 401, "Failed to fetch books")

        assertTrue(errorMsg.contains("Failed to fetch books: HTTP 401"))
        assertTrue(errorMsg.contains("[PGRST301]"))
        assertTrue(errorMsg.contains("Empty JWT is sent in Authorization header"))
    }

    @Test
    fun testExtractErrorMessageFromJwtExpired() {
        val postgrestJson = """{"code":"PGRST303","details":null,"hint":null,"message":"JWT expired"}"""
        val errorMsg = supabaseClient.extractErrorMessage(postgrestJson, 401, "Failed to fetch series")

        assertTrue(errorMsg.contains("Failed to fetch series: HTTP 401"))
        assertTrue(errorMsg.contains("[PGRST303]"))
        assertTrue(errorMsg.contains("JWT expired"))
    }

    @Test
    fun testExtractErrorMessageFallback() {
        val errorMsg = supabaseClient.extractErrorMessage("", 401, "Failed to fetch books")
        assertEquals("Failed to fetch books: HTTP 401", errorMsg)

        val invalidJsonError = supabaseClient.extractErrorMessage("Not a json response", 500, "Server error")
        assertEquals("Server error: HTTP 500", invalidJsonError)
    }

    @Test
    fun testIsTokenExpiredWithValidFutureClaim() {
        // Build JWT with exp far in future (1 hour from now)
        val futureExp = (System.currentTimeMillis() / 1000L) + 3600L
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"usr1","exp":$futureExp}""".toByteArray(StandardCharsets.UTF_8))
        val testJwt = "$header.$payload.dummySignature"

        val authRepo = AuthRepository(null, createMockPrefs())
        assertFalse("Future token should not be expired", authRepo.isTokenExpired(testJwt))
    }

    @Test
    fun testIsTokenExpiredWithPastClaim() {
        // Build JWT with exp in the past
        val pastExp = (System.currentTimeMillis() / 1000L) - 300L
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"usr1","exp":$pastExp}""".toByteArray(StandardCharsets.UTF_8))
        val testJwt = "$header.$payload.dummySignature"

        val authRepo = AuthRepository(null, createMockPrefs())
        assertTrue("Past token must report expired", authRepo.isTokenExpired(testJwt))
    }

    @Test
    fun testIsTokenExpiredWithMalformedToken() {
        val authRepo = AuthRepository(null, createMockPrefs())
        assertTrue("Blank token is expired", authRepo.isTokenExpired(""))
        assertTrue("Null token is expired", authRepo.isTokenExpired(null))
        assertTrue("Invalid segment token is expired", authRepo.isTokenExpired("invalid.token"))
    }

    private fun createMockPrefs(): android.content.SharedPreferences {
        val map = mutableMapOf<String, Any?>()
        val mockEditor = java.lang.reflect.Proxy.newProxyInstance(
            android.content.SharedPreferences.Editor::class.java.classLoader,
            arrayOf(android.content.SharedPreferences.Editor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "putString" -> { map[args[0] as String] = args[1]; null }
                "putLong" -> { map[args[0] as String] = args[1]; null }
                "clear" -> { map.clear(); null }
                "apply", "commit" -> null
                else -> null
            }
        } as android.content.SharedPreferences.Editor

        val mockPrefs = java.lang.reflect.Proxy.newProxyInstance(
            android.content.SharedPreferences::class.java.classLoader,
            arrayOf(android.content.SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getString" -> map[args[0] as String] ?: args[1]
                "getLong" -> map[args[0] as String] ?: args[1]
                "edit" -> mockEditor
                else -> null
            }
        } as android.content.SharedPreferences
        return mockPrefs
    }
}
