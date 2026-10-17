package com.example.digitallibrary

import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.DownloadedBook
import com.example.digitallibrary.data.model.ReadingProgress
import com.example.digitallibrary.data.model.Series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SeriesAndOfflineReadingTest {

    @Test
    fun testSeriesOrdering() {
        val series = Series(
            id = "series-123",
            title = "Annual Reports Series",
            isPublished = true
        )

        val book1 = Book(
            id = "b1",
            title = "Report Vol 1",
            fileType = "pdf",
            storagePath = "b1.pdf",
            seriesId = series.id,
            seriesOrder = 1
        )

        val book2 = Book(
            id = "b2",
            title = "Report Vol 2",
            fileType = "docx",
            storagePath = "b2.docx",
            seriesId = series.id,
            seriesOrder = 2
        )

        val book3 = Book(
            id = "b3",
            title = "Report Vol 3",
            fileType = "txt",
            storagePath = "b3.txt",
            seriesId = series.id,
            seriesOrder = 3
        )

        val unsortedBooks = listOf(book3, book1, book2)
        val sortedBooks = unsortedBooks.sortedBy { it.seriesOrder }

        assertEquals("Report Vol 1", sortedBooks[0].title)
        assertEquals("Report Vol 2", sortedBooks[1].title)
        assertEquals("Report Vol 3", sortedBooks[2].title)
    }

    @Test
    fun testIndependentBookBehavior() {
        val standaloneBook = Book(
            id = "standalone-1",
            title = "Organizational Bylaws 2026",
            fileType = "pdf",
            storagePath = "bylaws.pdf",
            seriesId = null,
            seriesOrder = 0
        )

        assertNull(standaloneBook.seriesId)
        assertEquals(0, standaloneBook.seriesOrder)
    }

    @Test
    fun testBookUpdateDetection() {
        val localDownload = DownloadedBook(
            bookId = "book-99",
            title = "Security Guidelines",
            author = "Security Team",
            description = null,
            fileType = "pdf",
            storagePath = "sec.pdf",
            localFilePath = "/mock/path/sec.pdf",
            fileSizeBytes = 1024 * 500,
            version = 1,
            contentHash = "hash-v1",
            parsedContent = null,
            downloadedAt = System.currentTimeMillis()
        )

        val remoteUpdatedBook = Book(
            id = "book-99",
            title = "Security Guidelines",
            fileType = "pdf",
            storagePath = "sec.pdf",
            version = 2,
            contentHash = "hash-v2"
        )

        val hasUpdate = remoteUpdatedBook.version > localDownload.version ||
                (remoteUpdatedBook.contentHash != null && remoteUpdatedBook.contentHash != localDownload.contentHash)

        assertTrue("Update must be detected when remote version is higher", hasUpdate)
    }

    @Test
    fun testProgressTimestampConflictResolution() {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val localTime = format.parse("2026-10-18T01:00:00")!!.time
        val remoteTime = format.parse("2026-10-18T02:30:00")!!.time

        val localProgress = ReadingProgress(
            bookId = "b1",
            userId = "u1",
            lastPage = 10,
            progressPercent = 25,
            clientUpdatedAt = "2026-10-18T01:00:00.000Z"
        )

        val remoteProgress = ReadingProgress(
            bookId = "b1",
            userId = "u1",
            lastPage = 25,
            progressPercent = 60,
            clientUpdatedAt = "2026-10-18T02:30:00.000Z",
            updatedAt = "2026-10-18T02:30:00.000Z"
        )

        // Conflict resolution: remote is newer
        val winningProgress = if (remoteTime > localTime) remoteProgress else localProgress
        assertEquals(25, winningProgress.lastPage)
        assertEquals(60, winningProgress.progressPercent)
    }

    @Test
    fun testFormattedFileSize() {
        val kbBook = DownloadedBook(
            bookId = "1",
            title = "Small File",
            author = null,
            description = null,
            fileType = "txt",
            storagePath = "sm.txt",
            localFilePath = "",
            fileSizeBytes = 512 * 1024,
            version = 1,
            contentHash = null,
            parsedContent = null,
            downloadedAt = 0
        )
        assertEquals("512.0 KB", kbBook.formattedFileSize)

        val mbBook = DownloadedBook(
            bookId = "2",
            title = "Large Manual",
            author = null,
            description = null,
            fileType = "pdf",
            storagePath = "lg.pdf",
            localFilePath = "",
            fileSizeBytes = (3.5 * 1024 * 1024).toLong(),
            version = 1,
            contentHash = null,
            parsedContent = null,
            downloadedAt = 0
        )
        assertEquals("3.50 MB", mbBook.formattedFileSize)
    }
}
