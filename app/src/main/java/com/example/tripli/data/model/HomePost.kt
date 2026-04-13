package com.example.tripli.data.model

data class HomePost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val timeAgo: String,
    val imageUrl: String,
    val location: String,
    val rating: Float,
    val title: String,
    val caption: String,
    val hashtags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
)
