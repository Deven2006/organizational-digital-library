package com.example.digitallibrary.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("id")
    val id: String,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("role")
    val role: String = "member",

    @SerialName("created_at")
    val createdAt: String? = null
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)
}

@Serializable
data class AuthUser(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String? = null
)

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("expires_in")
    val expiresIn: Long? = null,

    @SerialName("user")
    val user: AuthUser
)

@Serializable
data class AuthErrorResponse(
    @SerialName("error")
    val error: String? = null,

    @SerialName("error_description")
    val errorDescription: String? = null,

    @SerialName("msg")
    val msg: String? = null,

    @SerialName("message")
    val message: String? = null
)
