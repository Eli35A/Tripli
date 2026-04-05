package com.example.tripli.data.model

import androidx.annotation.DrawableRes

data class TravelPost(
    @DrawableRes val imageResId: Int,
    val location: String,
    val quote: String,
    val authorName: String,
    val authorInitial: String,
    val authorAccentHex: String
)
