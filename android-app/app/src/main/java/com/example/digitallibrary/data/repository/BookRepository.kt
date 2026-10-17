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
     * Online: fetches from Supabase and updates local cache.
     * Offline: falls back to locally cached books.
     */
    suspend fun getPublishedBooks(): Result<List<Book>> {
        val token = authRepository.getAuthToken()
        if (token == null) {
            // Return cached books if offline or unauthenticated
            val cached = dbHelper.getCachedBooks()
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IllegalStateException("User is not authenticated."))
            }
        }

        val remoteResult = supabaseClient.getPublishedBooks(token)
        return if (remoteResult.isSuccess) {
            val books = remoteResult.getOrThrow()
            dbHelper.cacheBooks(books)
            Result.success(books)
        } else {
            // Network failure fallback to local cache
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
        val token = authRepository.getAuthToken()
        if (token == null) {
            val cached = dbHelper.getCachedSeries()
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IllegalStateException("User is not authenticated."))
            }
        }

        val remoteResult = supabaseClient.getPublishedSeries(token)
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
     */
    suspend fun getSeriesBooks(seriesId: String): Result<List<Book>> {
        val token = authRepository.getAuthToken()
        if (token == null) {
            val cached = dbHelper.getCachedBooks().filter { it.seriesId == seriesId }
                .sortedBy { it.seriesOrder }
            return Result.success(cached)
        }

        val remoteResult = supabaseClient.getSeriesBooks(seriesId, token)
        return if (remoteResult.isSuccess) {
            remoteResult
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

        val token = authRepository.getAuthToken()
        if (token == null) {
            return if (cached != null) Result.success(cached)
            else Result.failure(IllegalStateException("User is not authenticated and book is not cached."))
        }

        val remoteResult = supabaseClient.getBookById(bookId, token)
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
        val token = authRepository.getAuthToken()
            ?: return Result.failure(IllegalStateException("User is not authenticated."))

        val cacheFolder = File(context.cacheDir, "book_documents")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs()
        }

        val localFile = File(cacheFolder, storagePath)
        if (localFile.exists() && localFile.length() > 0) {
            return Result.success(localFile)
        }

        return supabaseClient.downloadBookFile(storagePath, token, localFile)
    }
}
