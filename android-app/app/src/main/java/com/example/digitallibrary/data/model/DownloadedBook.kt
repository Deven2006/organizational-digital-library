package com.example.digitallibrary.data.model

import java.io.File

data class DownloadedBook(
    val bookId: String,
    val title: String,
    val author: String?,
    val description: String?,
    val fileType: String,
    val storagePath: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val version: Int,
    val contentHash: String?,
    val parsedContent: String?,
    val downloadedAt: Long,
    val seriesId: String? = null,
    val seriesOrder: Int = 0,
    val hasUpdateAvailable: Boolean = false,
    val latestRemoteVersion: Int = version
) {
    val file: File get() = File(localFilePath)
    val existsOnDisk: Boolean get() = file.exists() && file.length() > 0

    val isPdf: Boolean get() = fileType.equals("pdf", ignoreCase = true)
    val isDocx: Boolean get() = fileType.equals("docx", ignoreCase = true)
    val isTxt: Boolean get() = fileType.equals("txt", ignoreCase = true)

    val formattedFileSize: String
        get() {
            if (fileSizeBytes <= 0) return "0 KB"
            val kb = fileSizeBytes / 1024.0
            return if (kb < 1024) {
                String.format("%.1f KB", kb)
            } else {
                val mb = kb / 1024.0
                String.format("%.2f MB", mb)
            }
        }
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class InProgress(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Failed(val errorMessage: String) : DownloadState()
}
