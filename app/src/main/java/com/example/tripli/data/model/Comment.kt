package com.example.tripli.data.model

data class Comment(
    val id: String,
    val userName: String,
    val userInitial: String,
    val userAccentHex: String,
    val text: String,
    val timeAgo: String
)
