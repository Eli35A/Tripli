package com.example.tripli.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tripli.data.model.AppUser

@Entity(tableName = "users")
data class AppUserEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val localPhotoPath: String?,
    val bio: String?,
    val lastLoginAt: Long
) {
    fun toDomain() = AppUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        localPhotoPath = localPhotoPath,
        bio = bio
    )
}
