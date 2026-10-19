package com.example.digitallibrary.data.repository

import android.content.Context
import com.example.digitallibrary.data.local.LibraryDatabaseHelper
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.DownloadState
import com.example.digitallibrary.data.model.DownloadedBook
import com.example.digitallibrary.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class BatchDownloadProgress(
    val totalBooks: Int,
    val completedBooks: Int,
    val failedBooks: Int,
    val currentBookTitle: String,
    val isFinished: Boolean = false,
    val errorMessages: List<String> = emptyList()
)

class DownloadManager(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val dbHelper: LibraryDatabaseHelper = LibraryDatabaseHelper(context),
    private val supabaseClient: SupabaseClient = SupabaseClient()
) {
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val downloadsFolder = File(context.filesDir, "downloaded_books").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Downloads an individual book to persistent local storage.
     */
    suspend fun downloadBook(book: Book): Result<DownloadedBook> = withContext(Dispatchers.IO) {
        val token = authRepository.getValidAuthToken() ?: authRepository.getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("User is not authenticated. Please log in."))

        // 1. Storage safety check (require at least 25 MB usable space)
        val usableSpace = context.filesDir.usableSpace
        val minRequiredBytes = 25 * 1024 * 1024L
        if (usableSpace < minRequiredBytes) {
            val error = "Insufficient device storage space. Please free up space before downloading."
            updateBookState(book.id, DownloadState.Failed(error))
            return@withContext Result.failure(IOException(error))
        }

        updateBookState(book.id, DownloadState.InProgress(0, 0, 0))

        val extension = book.fileType.lowercase()
        val destinationFile = File(downloadsFolder, "${book.id}.$extension")

        try {
            // 2. Download the actual file content from private storage bucket
            val downloadResult = supabaseClient.downloadBookFile(
                storagePath = book.storagePath,
                authToken = token,
                destinationFile = destinationFile,
                onProgress = { downloaded, total ->
                    val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    updateBookState(book.id, DownloadState.InProgress(percent, downloaded, total))
                }
            )

            if (downloadResult.isFailure) {
                // Special fallback for DOCX/TXT: if storage file fails but parsed content exists, save text locally
                if ((book.isDocx || book.isTxt) && !book.parsedContent.isNullOrBlank()) {
                    FileOutputStream(destinationFile).use { fos ->
                        fos.write(book.parsedContent.toByteArray(Charsets.UTF_8))
                    }
                } else {
                    val errorMsg = downloadResult.exceptionOrNull()?.message ?: "Download failed."
                    updateBookState(book.id, DownloadState.Failed(errorMsg))
                    return@withContext Result.failure(IOException(errorMsg))
                }
            }

            // 3. Save metadata into persistent SQLite database
            val downloadedBook = DownloadedBook(
                bookId = book.id,
                title = book.title,
                author = book.author,
                description = book.description,
                fileType = book.fileType,
                storagePath = book.storagePath,
                localFilePath = destinationFile.absolutePath,
                fileSizeBytes = destinationFile.length(),
                version = book.version,
                contentHash = book.contentHash,
                parsedContent = book.parsedContent,
                downloadedAt = System.currentTimeMillis(),
                seriesId = book.seriesId,
                seriesOrder = book.seriesOrder,
                hasUpdateAvailable = false
            )

            dbHelper.saveDownloadedBook(downloadedBook)
            updateBookState(book.id, DownloadState.Completed(destinationFile))

            Result.success(downloadedBook)
        } catch (e: Exception) {
            updateBookState(book.id, DownloadState.Failed(e.message ?: "Download encountered an error."))
            Result.failure(e)
        }
    }

    /**
     * Downloads multiple books sequentially.
     * Partial failure resilience: A failure in one book does not cancel or delete successful books.
     */
    fun downloadMultipleBooks(books: List<Book>): Flow<BatchDownloadProgress> = flow {
        val total = books.size
        var completed = 0
        var failed = 0
        val errors = mutableListOf<String>()

        emit(BatchDownloadProgress(total, completed, failed, if (books.isNotEmpty()) books.first().title else "", false))

        for (book in books) {
            emit(BatchDownloadProgress(total, completed, failed, book.title, false, errors))
            val result = downloadBook(book)
            if (result.isSuccess) {
                completed++
            } else {
                failed++
                val err = "${book.title}: ${result.exceptionOrNull()?.message ?: "Failed"}"
                errors.add(err)
            }
            emit(BatchDownloadProgress(total, completed, failed, book.title, false, errors))
        }

        emit(BatchDownloadProgress(total, completed, failed, "Finished", true, errors))
    }.flowOn(Dispatchers.IO)

    /**
     * Downloads an entire series.
     */
    fun downloadSeries(booksInSeries: List<Book>): Flow<BatchDownloadProgress> {
        return downloadMultipleBooks(booksInSeries)
    }

    /**
     * Removes a downloaded book from local disk and database.
     */
    fun removeDownload(bookId: String): Boolean {
        _downloadStates.value = _downloadStates.value - bookId
        return dbHelper.deleteDownloadedBook(bookId)
    }

    /**
     * Checks if a book is downloaded locally on disk.
     */
    fun isBookDownloaded(bookId: String): Boolean {
        return dbHelper.isBookDownloaded(bookId)
    }

    /**
     * Retrieves all downloaded books from local database.
     */
    fun getDownloadedBooks(): List<DownloadedBook> {
        return dbHelper.getDownloadedBooks()
    }

    /**
     * Retrieves a single downloaded book if present on disk.
     */
    fun getDownloadedBook(bookId: String): DownloadedBook? {
        return dbHelper.getDownloadedBook(bookId)
    }

    /**
     * Detects if newer versions exist on server without discarding local files.
     */
    fun checkForUpdates(remoteBooks: List<Book>): List<DownloadedBook> {
        val localBooks = dbHelper.getDownloadedBooks()
        val remoteMap = remoteBooks.associateBy { it.id }

        return localBooks.map { local ->
            val remote = remoteMap[local.bookId]
            if (remote != null && (remote.version > local.version || (remote.contentHash != null && remote.contentHash != local.contentHash))) {
                local.copy(hasUpdateAvailable = true, latestRemoteVersion = remote.version)
            } else {
                local.copy(hasUpdateAvailable = false)
            }
        }
    }

    /**
     * Reconciles local downloads with authoritative server records.
     * Permanently deleted books (absent from existingRemoteBookMap) are safely removed.
     * Unpublished books (present with isPublished = false) are preserved.
     * Repeated runs are completely idempotent.
     */
    fun reconcileAuthoritativeDeletions(existingRemoteBookMap: Map<String, Boolean>): List<String> {
        val localBooks = dbHelper.getDownloadedBooks()
        if (localBooks.isEmpty()) return emptyList()

        val deletedIds = mutableListOf<String>()
        for (local in localBooks) {
            // If the book does NOT exist on the server at all, it was permanently deleted by an administrator
            if (!existingRemoteBookMap.containsKey(local.bookId)) {
                val removed = removeDownload(local.bookId)
                if (removed) {
                    deletedIds.add(local.bookId)
                }
            }
            // If it exists but isPublished == false, it is unpublished, so local file is safely preserved
        }
        return deletedIds
    }

    private fun updateBookState(bookId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value + (bookId to state)
    }
}

