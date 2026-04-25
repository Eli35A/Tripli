package com.example.tripli.data.model

import androidx.annotation.DrawableRes

data class HomePost(
    val id: String,
    val userName: String,
    val userInitial: String,
    val userAccentHex: String,
    val timeAgo: String,
    @DrawableRes val imageResId: Int,
    val location: String,
    val rating: Float,
    val title: String,
    val caption: String,
    val hashtags: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val comments: List<Comment> = emptyList()
)