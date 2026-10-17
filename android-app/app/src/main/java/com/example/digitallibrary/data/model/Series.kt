package com.example.digitallibrary.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Series(
    @SerialName("id")
    val id: String,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("cover_image_url")
    val coverImageUrl: String? = null,

    @SerialName("is_published")
    val isPublished: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
