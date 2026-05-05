package com.example.tripli.data.model

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val localPhotoPath: String? = null,
    val bio: String? = null
)
