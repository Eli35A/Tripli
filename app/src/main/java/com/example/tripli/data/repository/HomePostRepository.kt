package com.example.tripli.data.repository

import com.example.tripli.R
import com.example.tripli.data.model.HomePost

class HomePostRepository {

    fun getHomePosts(): List<HomePost> = listOf(
        HomePost(
            id = "1",
            userName = "Jane Cooper",
            userInitial = "J",
            userAccentHex = "#F4A460",
            timeAgo = "2 hours ago",
            imageResId = R.drawable.post_forest,
            location = "Ubud, Bali",
            rating = 5.0f,
            title = "Hidden Gem in Bali",
            caption = "Found this incredible spot away from the crowds. The hike down was worth every step!",
            hashtags = listOf("#bali", "#hiddenparadise", "#nature"),
            likeCount = 1200,
            commentCount = 45
        ),
        HomePost(
            id = "2",
            userName = "Alex Chen",
            userInitial = "A",
            userAccentHex = "#5BA4CF",
            timeAgo = "5 hours ago",
            imageResId = R.drawable.post_beach,
            location = "Nishiki Market, Kyoto",
            rating = 4.8f,
            title = "Street Food Heaven!",
            caption = "Must try the tako tamago (octopus inside a quail egg) at Nishiki Market. The flavors are unreal.",
            hashtags = listOf("#japan", "#foodie", "#kyoto"),
            likeCount = 856,
            commentCount = 23
        ),
        HomePost(
            id = "3",
            userName = "Maria Santos",
            userInitial = "M",
            userAccentHex = "#6DBF8A",
            timeAgo = "1 day ago",
            imageResId = R.drawable.post_mountain,
            location = "Swiss Alps",
            rating = 4.9f,
            title = "Above the Clouds",
            caption = "The cable car ride to the summit is breathtaking. Get there early to beat the clouds.",
            hashtags = listOf("#swissalps", "#hiking", "#mountains"),
            likeCount = 2100,
            commentCount = 87
        )
    )
}