package com.example.digitallibrary.data.repository

import android.content.Context
import com.example.digitallibrary.data.local.LibraryDatabaseHelper
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.DownloadedBook
import com.example.digitallibrary.data.model.Series
import com.example.digitallibrary.data.remote.SupabaseClient
import java.io.File
import java.io.IOException

class BookRepository(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val dbHelper: LibraryDatabaseHelper = LibraryDatabaseHelper(context),
    private val supabaseClient: SupabaseClient = SupabaseClient()
) {
    val downloadManager = DownloadManager(context, authRepository, dbHelper, supabaseClient)
    val progressRepository = ProgressRepository(context, authRepository, dbHelper, supabaseClient)

    /**
     * Retrieves all published books.
     * Online: fetches from Supabase with valid session and updates local cache.
     * Reconciles permanently deleted downloads upon successful sync.
     * Offline: falls back to locally cached books.
     */
    suspend fun getPublishedBooks(): Result<List<Book>> {
        var token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            val cached = dbHelper.getCachedBooks()
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IllegalStateException("User is not authenticated."))
            }
        }

        var remoteResult = supabaseClient.getPublishedBooks(token)
        // If HTTP 401, attempt token refresh and retry once
        if (remoteResult.isFailure && remoteResult.exceptionOrNull()?.message?.contains("401") == true) {
            val refreshResult = authRepository.refreshAuthToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrThrow()
                remoteResult = supabaseClient.getPublishedBooks(token)
            } else {
                // Token refresh failed; session is expired/revoked on server. Clear dead session.
                authRepository.logout()
            }
        }

        return if (remoteResult.isSuccess) {
            val books = remoteResult.getOrThrow()
            dbHelper.cacheBooks(books)
            // Reconcile downloads after successful authoritative synchronization
            try {
                reconcileDownloads()
            } catch (e: Exception) {
                // Non-fatal: do not abort library load if reconciliation encounters a transient issue
            }
            Result.success(books)
        } else {
            val cached = dbHelper.getCachedBooks()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                remoteResult
            }
        }
    }

    /**
     * Retrieves all published book series.
     * Online: fetches from Supabase and updates local cache.
     * Offline: falls back to locally cached series.
     */
    suspend fun getPublishedSeries(): Result<List<Series>> {
        var token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            val cached = dbHelper.getCachedSeries()
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IllegalStateException("User is not authenticated."))
            }
        }

        var remoteResult = supabaseClient.getPublishedSeries(token)
        if (remoteResult.isFailure && remoteResult.exceptionOrNull()?.message?.contains("401") == true) {
            val refreshResult = authRepository.refreshAuthToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrThrow()
                remoteResult = supabaseClient.getPublishedSeries(token)
            } else {
                authRepository.logout()
            }
        }

        return if (remoteResult.isSuccess) {
            val seriesList = remoteResult.getOrThrow()
            dbHelper.cacheSeries(seriesList)
            Result.success(seriesList)
        } else {
            val cached = dbHelper.getCachedSeries()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                remoteResult
            }
        }
    }

    /**
     * Retrieves all books belonging to a series, ordered by series_order.
     * Caches fetched series books locally for resilient offline access.
     */
    suspend fun getSeriesBooks(seriesId: String): Result<List<Book>> {
        var token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            val cached = dbHelper.getCachedBooks().filter { it.seriesId == seriesId }
                .sortedBy { it.seriesOrder }
            return Result.success(cached)
        }

        var remoteResult = supabaseClient.getSeriesBooks(seriesId, token)
        if (remoteResult.isFailure && remoteResult.exceptionOrNull()?.message?.contains("401") == true) {
            val refreshResult = authRepository.refreshAuthToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrThrow()
                remoteResult = supabaseClient.getSeriesBooks(seriesId, token)
            } else {
                authRepository.logout()
            }
        }

        return if (remoteResult.isSuccess) {
            val books = remoteResult.getOrThrow()
            dbHelper.cacheBooks(books)
            Result.success(books)
        } else {
            val cached = dbHelper.getCachedBooks().filter { it.seriesId == seriesId }
                .sortedBy { it.seriesOrder }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                remoteResult
            }
        }
    }

    /**
     * Reconciles local downloads against authoritative server existence.
     * Safely deletes local copies of permanently deleted server books.
     * Never deletes on network error, HTTP 401/403, or unpublishing.
     */
    suspend fun reconcileDownloads(): Result<List<String>> {
        val localDownloads = dbHelper.getDownloadedBooks()
        if (localDownloads.isEmpty()) return Result.success(emptyList())

        var token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Cannot reconcile downloads while unauthenticated."))
        }

        val localIds = localDownloads.map { it.bookId }
        var checkResult = supabaseClient.checkExistingBooks(localIds, token)
        if (checkResult.isFailure && checkResult.exceptionOrNull()?.message?.contains("401") == true) {
            val refreshResult = authRepository.refreshAuthToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrThrow()
                checkResult = supabaseClient.checkExistingBooks(localIds, token)
            } else {
                authRepository.logout()
            }
        }

        if (checkResult.isFailure) {
            // Network failure or unauthenticated: preserve all downloads safely!
            return Result.failure(checkResult.exceptionOrNull() ?: Exception("Failed to check book existence."))
        }

        val existingMap = checkResult.getOrThrow()
        val deletedIds = downloadManager.reconcileAuthoritativeDeletions(existingMap)
        return Result.success(deletedIds)
    }

    /**
     * Retrieves a single book by ID.
     * Checks local downloaded books first, then cached books, then remote.
     */
    suspend fun getBookById(bookId: String): Result<Book> {
        // 1. Check if downloaded locally
        val downloaded = downloadManager.getDownloadedBook(bookId)
        if (downloaded != null) {
            return Result.success(
                Book(
                    id = downloaded.bookId,
                    title = downloaded.title,
                    author = downloaded.author,
                    description = downloaded.description,
                    fileType = downloaded.fileType,
                    storagePath = downloaded.storagePath,
                    parsedContent = downloaded.parsedContent,
                    isPublished = true,
                    seriesId = downloaded.seriesId,
                    seriesOrder = downloaded.seriesOrder,
                    version = downloaded.version,
                    contentHash = downloaded.contentHash
                )
            )
        }

        // 2. Check local cache
        val cached = dbHelper.getCachedBooks().find { it.id == bookId }

        var token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            return if (cached != null) Result.success(cached)
            else Result.failure(IllegalStateException("User is not authenticated and book is not cached."))
        }

        var remoteResult = supabaseClient.getBookById(bookId, token)
        if (remoteResult.isFailure && remoteResult.exceptionOrNull()?.message?.contains("401") == true) {
            val refreshResult = authRepository.refreshAuthToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrThrow()
                remoteResult = supabaseClient.getBookById(bookId, token)
            } else {
                authRepository.logout()
            }
        }

        return if (remoteResult.isSuccess) {
            remoteResult
        } else {
            if (cached != null) Result.success(cached) else remoteResult
        }
    }

    /**
     * Retrieves the persistent local file for a book if downloaded.
     */
    fun getLocalFileForBook(bookId: String): File? {
        val downloaded = downloadManager.getDownloadedBook(bookId)
        return if (downloaded != null && downloaded.existsOnDisk) downloaded.file else null
    }

    /**
     * Securely downloads a book document into local cache for reading if not already downloaded.
     */
    suspend fun downloadDocument(storagePath: String): Result<File> {
        var token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
        if (token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("User is not authenticated."))
        }

        val cacheFolder = File(context.cacheDir, "book_documents")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs()
        }

        val localFile = File(cacheFolder, storagePath)
        if (localFile.exists() && localFile.length() > 0) {
            return Result.success(localFile)
        }

        var downloadResult = supabaseClient.downloadBookFile(storagePath, token, localFile)
        if (downloadResult.isFailure && downloadResult.exceptionOrNull()?.message?.contains("401") == true) {
            val refreshResult = authRepository.refreshAuthToken()
            if (refreshResult.isSuccess) {
                token = refreshResult.getOrThrow()
                downloadResult = supabaseClient.downloadBookFile(storagePath, token, localFile)
            } else {
                authRepository.logout()
            }
        }

        return downloadResult
    }
}
