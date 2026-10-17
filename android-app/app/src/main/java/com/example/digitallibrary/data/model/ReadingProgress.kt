package com.example.digitallibrary.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReadingProgress(
    @SerialName("book_id")
    val bookId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("progress_percent")
    val progressPercent: Int = 0,

    @SerialName("last_page")
    val lastPage: Int = 0,

    @SerialName("is_completed")
    val isCompleted: Boolean = false,

    @SerialName("client_updated_at")
    val clientUpdatedAt: String,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
