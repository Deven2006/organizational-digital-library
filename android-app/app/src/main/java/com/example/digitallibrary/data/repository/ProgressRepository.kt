package com.example.digitallibrary.data.repository

import android.content.Context
import com.example.digitallibrary.data.local.LibraryDatabaseHelper
import com.example.digitallibrary.data.model.ReadingProgress
import com.example.digitallibrary.data.remote.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ProgressRepository(
    context: Context,
    private val authRepository: AuthRepository,
    private val dbHelper: LibraryDatabaseHelper = LibraryDatabaseHelper(context),
    private val supabaseClient: SupabaseClient = SupabaseClient()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getCurrentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    /**
     * Saves reading progress immediately to local SQLite database and asynchronously
     * synchronizes with Supabase when online.
     */
    fun saveProgress(
        bookId: String,
        lastPage: Int,
        progressPercent: Int,
        isCompleted: Boolean
    ) {
        val userId = authRepository.getUserId() ?: return
        val progress = ReadingProgress(
            bookId = bookId,
            userId = userId,
            progressPercent = progressPercent.coerceIn(0, 100),
            lastPage = lastPage.coerceAtLeast(0),
            isCompleted = isCompleted,
            clientUpdatedAt = getCurrentIsoTimestamp()
        )

        // 1. Immediate local persistence
        dbHelper.saveReadingProgress(progress, isSynced = false)

        // 2. Attempt remote sync in background
        scope.launch {
            val token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
            if (token.isNullOrBlank()) return@launch
            val result = supabaseClient.upsertReadingProgress(progress, token)
            if (result.isSuccess) {
                dbHelper.markProgressSynced(bookId, userId)
            }
        }
    }

    /**
     * Retrieves reading progress from local persistent store (zero network latency, 100% offline).
     */
    fun getLocalProgress(bookId: String): ReadingProgress? {
        val userId = authRepository.getUserId() ?: return null
        return dbHelper.getReadingProgress(bookId, userId)
    }

    /**
     * Retrieves all local reading progress records for current user.
     */
    fun getAllLocalProgress(): List<ReadingProgress> {
        val userId = authRepository.getUserId() ?: return emptyList()
        return dbHelper.getAllReadingProgress(userId)
    }

    /**
     * Synchronizes reading progress with Supabase.
     * Implements two-way conflict resolution: Newer timestamp wins.
     */
    suspend fun syncWithRemote(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authRepository.getUserId()
            ?: return@withContext Result.failure(IllegalStateException("User is not authenticated."))
        val token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No authentication token."))
        }

        try {
            // 1. Push unsynced local changes to remote
            val unsynced = dbHelper.getUnsyncedReadingProgress(userId)
            for (local in unsynced) {
                val pushResult = supabaseClient.upsertReadingProgress(local, token)
                if (pushResult.isSuccess) {
                    dbHelper.markProgressSynced(local.bookId, userId)
                }
            }

            // 2. Pull remote records to incorporate updates from other devices
            val remoteResult = supabaseClient.getRemoteReadingProgress(userId, token)
            if (remoteResult.isSuccess) {
                val remoteList = remoteResult.getOrThrow()
                for (remote in remoteList) {
                    val local = dbHelper.getReadingProgress(remote.bookId, userId)
                    if (local == null) {
                        dbHelper.saveReadingProgress(remote, isSynced = true)
                    } else {
                        // Compare timestamps: if remote is newer, update local
                        val remoteTime = parseIsoTimestamp(remote.updatedAt ?: remote.clientUpdatedAt)
                        val localTime = parseIsoTimestamp(local.clientUpdatedAt)
                        if (remoteTime > localTime) {
                            dbHelper.saveReadingProgress(remote, isSynced = true)
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIsoTimestamp(isoString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoString.take(19))?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
