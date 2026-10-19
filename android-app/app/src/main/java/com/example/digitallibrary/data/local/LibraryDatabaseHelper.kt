package com.example.digitallibrary.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.DownloadedBook
import com.example.digitallibrary.data.model.ReadingProgress
import com.example.digitallibrary.data.model.Series
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LibraryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        const val DATABASE_NAME = "digital_library_local.db"
        const val DATABASE_VERSION = 1

        // Tables
        const val TABLE_DOWNLOADED_BOOKS = "downloaded_books"
        const val TABLE_CACHED_BOOKS = "cached_books"
        const val TABLE_CACHED_SERIES = "cached_series"
        const val TABLE_READING_PROGRESS = "reading_progress"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_DOWNLOADED_BOOKS (
                book_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                author TEXT,
                description TEXT,
                file_type TEXT NOT NULL,
                storage_path TEXT NOT NULL,
                local_file_path TEXT NOT NULL,
                file_size_bytes INTEGER NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                content_hash TEXT,
                parsed_content TEXT,
                downloaded_at INTEGER NOT NULL,
                series_id TEXT,
                series_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CACHED_BOOKS (
                book_id TEXT PRIMARY KEY,
                json_data TEXT NOT NULL,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CACHED_SERIES (
                series_id TEXT PRIMARY KEY,
                json_data TEXT NOT NULL,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_READING_PROGRESS (
                book_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                progress_percent INTEGER NOT NULL,
                last_page INTEGER NOT NULL,
                is_completed INTEGER NOT NULL,
                local_updated_at INTEGER NOT NULL,
                is_synced INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (book_id, user_id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADED_BOOKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_BOOKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_SERIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_READING_PROGRESS")
        onCreate(db)
    }

    // --------------------------------------------------------------------------
    // DOWNLOADED BOOKS
    // --------------------------------------------------------------------------
    fun saveDownloadedBook(book: DownloadedBook) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("book_id", book.bookId)
            put("title", book.title)
            put("author", book.author)
            put("description", book.description)
            put("file_type", book.fileType)
            put("storage_path", book.storagePath)
            put("local_file_path", book.localFilePath)
            put("file_size_bytes", book.fileSizeBytes)
            put("version", book.version)
            put("content_hash", book.contentHash)
            put("parsed_content", book.parsedContent)
            put("downloaded_at", book.downloadedAt)
            put("series_id", book.seriesId)
            put("series_order", book.seriesOrder)
        }
        db.insertWithOnConflict(TABLE_DOWNLOADED_BOOKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getDownloadedBooks(): List<DownloadedBook> {
        val list = mutableListOf<DownloadedBook>()
        val db = readableDatabase
        val cursor = db.query(TABLE_DOWNLOADED_BOOKS, null, null, null, null, null, "downloaded_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                val downloadedBook = cursorToDownloadedBook(it)
                // Only return if file actually exists on persistent disk
                if (downloadedBook.existsOnDisk) {
                    list.add(downloadedBook)
                }
            }
        }
        return list
    }

    fun getDownloadedBook(bookId: String): DownloadedBook? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DOWNLOADED_BOOKS,
            null,
            "book_id = ?",
            arrayOf(bookId),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToNext()) {
                val book = cursorToDownloadedBook(it)
                return if (book.existsOnDisk) book else null
            }
        }
        return null
    }

    fun deleteDownloadedBook(bookId: String): Boolean {
        val db = writableDatabase
        // 1. Direct query for local_file_path so file is deleted even if model checks had issues
        val cursor = db.query(
            TABLE_DOWNLOADED_BOOKS,
            arrayOf("local_file_path"),
            "book_id = ?",
            arrayOf(bookId),
            null,
            null,
            null
        )
        var filePath: String? = null
        cursor.use {
            if (it.moveToNext()) {
                filePath = it.getString(0)
            }
        }
        if (!filePath.isNullOrBlank()) {
            try {
                val f = File(filePath)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: Exception) {
                // Ignore failure on already removed or missing file
            }
        }

        // 2. Remove downloaded metadata
        val rows = db.delete(TABLE_DOWNLOADED_BOOKS, "book_id = ?", arrayOf(bookId))
        // 3. Purge cached book entry to remove stale references
        db.delete(TABLE_CACHED_BOOKS, "book_id = ?", arrayOf(bookId))
        return rows > 0
    }

    fun isBookDownloaded(bookId: String): Boolean {
        val book = getDownloadedBook(bookId)
        return book != null && book.existsOnDisk
    }

    private fun cursorToDownloadedBook(cursor: Cursor): DownloadedBook {
        return DownloadedBook(
            bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            author = cursor.getString(cursor.getColumnIndexOrThrow("author")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
            fileType = cursor.getString(cursor.getColumnIndexOrThrow("file_type")),
            storagePath = cursor.getString(cursor.getColumnIndexOrThrow("storage_path")),
            localFilePath = cursor.getString(cursor.getColumnIndexOrThrow("local_file_path")),
            fileSizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow("file_size_bytes")),
            version = cursor.getInt(cursor.getColumnIndexOrThrow("version")),
            contentHash = cursor.getString(cursor.getColumnIndexOrThrow("content_hash")),
            parsedContent = cursor.getString(cursor.getColumnIndexOrThrow("parsed_content")),
            downloadedAt = cursor.getLong(cursor.getColumnIndexOrThrow("downloaded_at")),
            seriesId = cursor.getString(cursor.getColumnIndexOrThrow("series_id")),
            seriesOrder = cursor.getInt(cursor.getColumnIndexOrThrow("series_order"))
        )
    }

    // --------------------------------------------------------------------------
    // CACHED BOOKS (For offline browsing)
    // --------------------------------------------------------------------------
    fun cacheBooks(books: List<Book>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            for (book in books) {
                val values = ContentValues().apply {
                    put("book_id", book.id)
                    put("json_data", json.encodeToString(book))
                    put("cached_at", now)
                }
                db.insertWithOnConflict(TABLE_CACHED_BOOKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCachedBooks(): List<Book> {
        val list = mutableListOf<Book>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CACHED_BOOKS, null, null, null, null, null, "cached_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                val jsonStr = it.getString(it.getColumnIndexOrThrow("json_data"))
                try {
                    val book = json.decodeFromString<Book>(jsonStr)
                    list.add(book)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    // --------------------------------------------------------------------------
    // CACHED SERIES (For offline browsing)
    // --------------------------------------------------------------------------
    fun cacheSeries(seriesList: List<Series>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            for (s in seriesList) {
                val values = ContentValues().apply {
                    put("series_id", s.id)
                    put("json_data", json.encodeToString(s))
                    put("cached_at", now)
                }
                db.insertWithOnConflict(TABLE_CACHED_SERIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCachedSeries(): List<Series> {
        val list = mutableListOf<Series>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CACHED_SERIES, null, null, null, null, null, "cached_at DESC")
        cursor.use {
            while (it.moveToNext()) {
                val jsonStr = it.getString(it.getColumnIndexOrThrow("json_data"))
                try {
                    val s = json.decodeFromString<Series>(jsonStr)
                    list.add(s)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    // --------------------------------------------------------------------------
    // READING PROGRESS
    // --------------------------------------------------------------------------
    fun saveReadingProgress(progress: ReadingProgress, isSynced: Boolean = false) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("book_id", progress.bookId)
            put("user_id", progress.userId)
            put("progress_percent", progress.progressPercent)
            put("last_page", progress.lastPage)
            put("is_completed", if (progress.isCompleted) 1 else 0)
            put("local_updated_at", System.currentTimeMillis())
            put("is_synced", if (isSynced) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_READING_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getReadingProgress(bookId: String, userId: String): ReadingProgress? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_READING_PROGRESS,
            null,
            "book_id = ? AND user_id = ?",
            arrayOf(bookId, userId),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToNext()) {
                return cursorToReadingProgress(it)
            }
        }
        return null
    }

    fun getAllReadingProgress(userId: String): List<ReadingProgress> {
        val list = mutableListOf<ReadingProgress>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_READING_PROGRESS,
            null,
            "user_id = ?",
            arrayOf(userId),
            null,
            null,
            "local_updated_at DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToReadingProgress(it))
            }
        }
        return list
    }

    fun getUnsyncedReadingProgress(userId: String): List<ReadingProgress> {
        val list = mutableListOf<ReadingProgress>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_READING_PROGRESS,
            null,
            "user_id = ? AND is_synced = 0",
            arrayOf(userId),
            null,
            null,
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToReadingProgress(it))
            }
        }
        return list
    }

    fun markProgressSynced(bookId: String, userId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_synced", 1)
        }
        db.update(
            TABLE_READING_PROGRESS,
            values,
            "book_id = ? AND user_id = ?",
            arrayOf(bookId, userId)
        )
    }

    private fun cursorToReadingProgress(cursor: Cursor): ReadingProgress {
        val updatedAtMillis = cursor.getLong(cursor.getColumnIndexOrThrow("local_updated_at"))
        return ReadingProgress(
            bookId = cursor.getString(cursor.getColumnIndexOrThrow("book_id")),
            userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
            progressPercent = cursor.getInt(cursor.getColumnIndexOrThrow("progress_percent")),
            lastPage = cursor.getInt(cursor.getColumnIndexOrThrow("last_page")),
            isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1,
            clientUpdatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(updatedAtMillis))
        )
    }
}
