package com.example.tripli.model

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val localPhotoPath: String? = null,
    val bio: String? = null
)
