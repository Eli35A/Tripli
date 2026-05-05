package com.example.tripli.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tripli.data.model.HomePost
import com.example.tripli.utils.TimeUtils
import java.io.File

@Entity(tableName = "home_posts")
data class HomePostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val createdAt: Long,
    val imageUrl: String,
    val localImagePath: String?,
    val location: String,
    val rating: Float,
    val title: String,
    val caption: String,
    val hashtags: String,        // pipe-separated e.g. "bali|travel|nature"
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val cachedAt: Long
) {
    fun toModel(): HomePost {
        val validLocalPath = localImagePath?.takeIf { File(it).exists() }
        return HomePost(
            id = id,
            authorId = authorId,
            authorName = authorName,
            authorPhotoUrl = authorPhotoUrl,
            timeAgo = TimeUtils.timeAgo(createdAt),
            imageUrl = imageUrl,
            localImagePath = validLocalPath,
            location = location,
            rating = rating,
            title = title,
            caption = caption,
            hashtags = if (hashtags.isBlank()) emptyList() else hashtags.split("|"),
            likeCount = likeCount,
            commentCount = commentCount,
            isLiked = isLiked,
            isSaved = isSaved
        )
    }
}
