package com.example.tripli.data.model

data class Comment(
    val id: String,
    val authorId: String,
    val userName: String,
    val authorPhotoUrl: String?,
    val text: String,
    val timeAgo: String
)
