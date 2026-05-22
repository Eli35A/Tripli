package com.example.tripli.data.repository.featured

import com.example.tripli.R
import com.example.tripli.model.TravelPost

class FeaturedPostsRepository {

    fun getFeaturedPosts(): List<TravelPost> {
        return listOf(
            TravelPost(
                imageResId = R.drawable.post_beach,
                location = "Bali, Indonesia",
                quote = "Found this hidden cove just 20 mins from the main strip!",
                authorName = "Sarah J.",
                authorInitial = "S",
                authorAccentHex = "#F4B183"
            ),
            TravelPost(
                imageResId = R.drawable.post_mountain,
                location = "Swiss Alps",
                quote = "The view after the cable car ride is worth every step.",
                authorName = "Marco P.",
                authorInitial = "M",
                authorAccentHex = "#9ED7E5"
            ),
            TravelPost(
                imageResId = R.drawable.post_forest,
                location = "Costa Rica",
                quote = "Best sunrise trail ever. Bring coffee and get here early.",
                authorName = "Nina R.",
                authorInitial = "N",
                authorAccentHex = "#A6D59A"
            )
        )
    }
}
