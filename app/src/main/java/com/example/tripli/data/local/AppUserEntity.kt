package com.example.tripli.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class AppUserEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val lastLoginAt: Long
)
