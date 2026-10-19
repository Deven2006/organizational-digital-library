package com.example.digitallibrary.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Book(
    @SerialName("id")
    val id: String,

    @SerialName("title")
    val title: String,

    @SerialName("author")
    val author: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("file_type")
    val fileType: String,

    @SerialName("storage_path")
    val storagePath: String,

    @SerialName("parsed_content")
    val parsedContent: String? = null,

    @SerialName("is_published")
    val isPublished: Boolean = false,

    @SerialName("series_id")
    val seriesId: String? = null,

    @SerialName("series_order")
    val seriesOrder: Int = 0,

    @SerialName("version")
    val version: Int = 1,

    @SerialName("content_hash")
    val contentHash: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val isPdf: Boolean get() = fileType.equals("pdf", ignoreCase = true)
    val isDocx: Boolean get() = fileType.equals("docx", ignoreCase = true)
    val isTxt: Boolean get() = fileType.equals("txt", ignoreCase = true)
}

@Serializable
data class BookExistence(
    @SerialName("id")
    val id: String,

    @SerialName("is_published")
    val isPublished: Boolean = false
)
