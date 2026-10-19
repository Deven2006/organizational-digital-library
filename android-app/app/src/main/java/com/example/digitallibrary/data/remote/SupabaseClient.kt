package com.example.digitallibrary.data.remote

import com.example.digitallibrary.data.model.AuthErrorResponse
import com.example.digitallibrary.data.model.AuthTokenResponse
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.BookExistence
import com.example.digitallibrary.data.model.Profile
import com.example.digitallibrary.data.model.ReadingProgress
import com.example.digitallibrary.data.model.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(): String {
        return SupabaseConfig.SUPABASE_URL.trimEnd('/')
    }

    private fun getAnonKey(): String {
        return SupabaseConfig.SUPABASE_ANON_KEY.trim()
    }

    /**
     * Normalizes and strips accidental Bearer prefixes or whitespace.
     */
    internal fun sanitizeAuthToken(authToken: String): String {
        var token = authToken.trim()
        if (token.startsWith("Bearer", ignoreCase = true)) {
            token = token.substring(6).trim()
        }
        return token
    }

    /**
     * Validates that the auth token is present and not the anon/publishable key.
     */
    internal fun validateAuthToken(authToken: String): Result<String> {
        val cleanToken = sanitizeAuthToken(authToken)
        if (cleanToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Authentication token is blank or empty."))
        }
        if (cleanToken == getAnonKey()) {
            return Result.failure(IllegalArgumentException("Supabase publishable anon key cannot be used as user Authorization Bearer JWT."))
        }
        return Result.success(cleanToken)
    }

    /**
     * Extracts descriptive error message from Supabase/PostgREST response body without leaking sensitive tokens.
     */
    internal fun extractErrorMessage(responseBody: String, code: Int, action: String): String {
        if (responseBody.isBlank()) return "$action: HTTP $code"
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            if (jsonElement is JsonObject) {
                val message = jsonElement["message"]?.jsonPrimitive?.contentOrNull
                    ?: jsonElement["error_description"]?.jsonPrimitive?.contentOrNull
                    ?: jsonElement["msg"]?.jsonPrimitive?.contentOrNull
                    ?: jsonElement["error"]?.jsonPrimitive?.contentOrNull
                val hint = jsonElement["hint"]?.jsonPrimitive?.contentOrNull
                val pgrstCode = jsonElement["code"]?.jsonPrimitive?.contentOrNull

                buildString {
                    append("$action: HTTP $code")
                    if (!pgrstCode.isNullOrBlank()) append(" [$pgrstCode]")
                    if (!message.isNullOrBlank()) append(" - $message")
                    if (!hint.isNullOrBlank()) append(" (Hint: $hint)")
                }
            } else {
                "$action: HTTP $code"
            }
        } catch (e: Exception) {
            "$action: HTTP $code"
        }
    }

    /**
     * Authenticates a user with email and password using Supabase Auth endpoint.
     */
    suspend fun signInWithPassword(email: String, password: String): Result<AuthTokenResponse> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials are not configured in SupabaseConfig.kt."))
        }

        try {
            val url = "${getBaseUrl()}/auth/v1/token?grant_type=password"
            val bodyJson = """{"email":"${email.trim()}","password":"$password"}"""
            val requestBody = bodyJson.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", getAnonKey())
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val tokenResponse = json.decodeFromString<AuthTokenResponse>(responseBody)
                    Result.success(tokenResponse)
                } else {
                    val errorMsg = try {
                        val errObj = json.decodeFromString<AuthErrorResponse>(responseBody)
                        errObj.errorDescription ?: errObj.message ?: errObj.msg ?: "Authentication error (${response.code})"
                    } catch (e: Exception) {
                        extractErrorMessage(responseBody, response.code, "Authentication failed")
                    }
                    Result.failure(IOException(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes an authenticated session using the refresh token.
     */
    suspend fun refreshToken(refreshToken: String): Result<AuthTokenResponse> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials are not configured in SupabaseConfig.kt."))
        }

        val cleanRefreshToken = refreshToken.trim()
        if (cleanRefreshToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Refresh token is blank or empty."))
        }

        try {
            val url = "${getBaseUrl()}/auth/v1/token?grant_type=refresh_token"
            val bodyJson = """{"refresh_token":"$cleanRefreshToken"}"""
            val requestBody = bodyJson.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", getAnonKey())
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val tokenResponse = json.decodeFromString<AuthTokenResponse>(responseBody)
                    Result.success(tokenResponse)
                } else {
                    val errorMsg = try {
                        val errObj = json.decodeFromString<AuthErrorResponse>(responseBody)
                        errObj.errorDescription ?: errObj.message ?: errObj.msg ?: "Token refresh error (${response.code})"
                    } catch (e: Exception) {
                        extractErrorMessage(responseBody, response.code, "Token refresh failed")
                    }
                    Result.failure(IOException(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches user profile from profiles table using PostgREST.
     */
    suspend fun getProfile(userId: String, authToken: String): Result<Profile> = withContext(Dispatchers.IO) {
        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/profiles?id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val profiles = json.decodeFromString<List<Profile>>(responseBody)
                    if (profiles.isNotEmpty()) {
                        Result.success(profiles.first())
                    } else {
                        Result.success(Profile(id = userId, fullName = null, role = "member"))
                    }
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to fetch profile")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all published books from books table.
     * Members can only retrieve books where is_published = true due to server-side RLS.
     */
    suspend fun getPublishedBooks(authToken: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
        }

        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/books?is_published=eq.true&order=created_at.desc&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val books = json.decodeFromString<List<Book>>(responseBody)
                    Result.success(books)
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to fetch books")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all published book series.
     */
    suspend fun getPublishedSeries(authToken: String): Result<List<Series>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
        }

        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/series?is_published=eq.true&order=created_at.desc&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val seriesList = json.decodeFromString<List<Series>>(responseBody)
                    Result.success(seriesList)
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to fetch series")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all published books belonging to a specific series, ordered by series_order.
     */
    suspend fun getSeriesBooks(seriesId: String, authToken: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
        }

        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/books?series_id=eq.$seriesId&is_published=eq.true&order=series_order.asc&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val books = json.decodeFromString<List<Book>>(responseBody)
                    Result.success(books)
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to fetch series books")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reconciles book existence authoritatively against Supabase.
     * Returns a map of bookId -> isPublished for all books that still exist on the server.
     * Any bookId absent from the returned map has been permanently deleted from the database.
     */
    suspend fun checkExistingBooks(bookIds: List<String>, authToken: String): Result<Map<String, Boolean>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured."))
        }
        if (bookIds.isEmpty()) {
            return@withContext Result.success(emptyMap())
        }

        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            // 1. First attempt: call check_existing_books RPC
            val rpcUrl = "${getBaseUrl()}/rest/v1/rpc/check_existing_books"
            val idsJsonArray = bookIds.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
            val bodyJson = """{"book_ids":$idsJsonArray}"""
            val requestBody = bodyJson.toRequestBody(jsonMediaType)

            val rpcRequest = Request.Builder()
                .url(rpcUrl)
                .post(requestBody)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .addHeader("Content-Type", "application/json")
                .build()

            val rpcResult = try {
                client.newCall(rpcRequest).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val existList = json.decodeFromString<List<BookExistence>>(responseBody)
                        existList.associate { it.id to it.isPublished }
                    } else null
                }
            } catch (e: Exception) {
                null
            }

            if (rpcResult != null) {
                return@withContext Result.success(rpcResult)
            }

            // 2. Fallback: query books table directly with in clause
            val inClause = bookIds.joinToString(separator = ",", prefix = "(", postfix = ")")
            val tableUrl = "${getBaseUrl()}/rest/v1/books?id=in.$inClause&select=id,is_published"
            val tableRequest = Request.Builder()
                .url(tableUrl)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(tableRequest).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val existList = json.decodeFromString<List<BookExistence>>(responseBody)
                    val map = existList.associate { it.id to it.isPublished }
                    Result.success(map)
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Existence check failed")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches a single book by ID.
     */
    suspend fun getBookById(bookId: String, authToken: String): Result<Book> = withContext(Dispatchers.IO) {
        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/books?id=eq.$bookId&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val books = json.decodeFromString<List<Book>>(responseBody)
                    if (books.isNotEmpty()) {
                        Result.success(books.first())
                    } else {
                        Result.failure(NoSuchElementException("Book not found or access denied."))
                    }
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to fetch book")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a private book document from the 'book-files' Supabase Storage bucket with streaming progress.
     */
    suspend fun downloadBookFile(
        storagePath: String,
        authToken: String,
        destinationFile: File,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/storage/v1/object/authenticated/book-files/$storagePath"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IOException(extractErrorMessage(responseBody, response.code, "Failed to download document"))
                    )
                }

                val body = response.body ?: return@withContext Result.failure(
                    IOException("Empty response body received from storage.")
                )

                val contentLength = body.contentLength()
                destinationFile.parentFile?.mkdirs()

                // Use staging file to avoid partial/corrupted files if interrupted
                val tempFile = File("${destinationFile.absolutePath}.tmp")
                try {
                    FileOutputStream(tempFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                onProgress?.invoke(totalBytesRead, contentLength)
                            }
                            output.flush()
                        }
                    }

                    // Atomic rename from temp to destination
                    if (destinationFile.exists()) {
                        destinationFile.delete()
                    }
                    if (!tempFile.renameTo(destinationFile)) {
                        // Fallback copy if rename across filesystems fails
                        tempFile.copyTo(destinationFile, overwrite = true)
                        tempFile.delete()
                    }

                    Result.success(destinationFile)
                } catch (e: Exception) {
                    tempFile.delete()
                    throw e
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upserts reading progress to Supabase with timestamp verification.
     */
    suspend fun upsertReadingProgress(
        progress: ReadingProgress,
        authToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/reading_progress?on_conflict=user_id,book_id"
            val progressJson = json.encodeToString(progress)
            val requestBody = progressJson.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 204) {
                    Result.success(Unit)
                } else {
                    val responseBody = response.body?.string().orEmpty()
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to sync reading progress")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all remote reading progress for a user.
     */
    suspend fun getRemoteReadingProgress(
        userId: String,
        authToken: String
    ): Result<List<ReadingProgress>> = withContext(Dispatchers.IO) {
        val tokenValidation = validateAuthToken(authToken)
        if (tokenValidation.isFailure) {
            return@withContext Result.failure(tokenValidation.exceptionOrNull()!!)
        }
        val cleanToken = tokenValidation.getOrThrow()

        try {
            val url = "${getBaseUrl()}/rest/v1/reading_progress?user_id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $cleanToken")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val progressList = json.decodeFromString<List<ReadingProgress>>(responseBody)
                    Result.success(progressList)
                } else {
                    Result.failure(IOException(extractErrorMessage(responseBody, response.code, "Failed to fetch remote reading progress")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
