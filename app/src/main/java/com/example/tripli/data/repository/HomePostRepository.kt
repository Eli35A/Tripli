package com.example.tripli.data.repository

import com.example.tripli.data.model.Comment
import com.example.tripli.data.model.HomePost
import com.example.tripli.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class HomePostRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val POSTS = "posts"
        private const val LIKES = "likes"
        private const val SAVES = "saves"
        private const val COMMENTS = "comments"

        private val AVATAR_COLORS = listOf(
            "#F4A460", "#5BA4CF", "#6DBF8A",
            "#B97FF5", "#F47C7C", "#FFB347"
        )

        fun colorForId(id: String): String =
            AVATAR_COLORS[Math.abs(id.hashCode()) % AVATAR_COLORS.size]
    }

    suspend fun getHomePosts(): List<HomePost> {
        val currentUserId = auth.currentUser?.uid
        val snapshot = firestore.collection(POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        return coroutineScope {
            snapshot.documents.map { doc ->
                async {
                    val isLiked = if (currentUserId != null) {
                        doc.reference.collection(LIKES).document(currentUserId).get().await().exists()
                    } else false
                    val isSaved = if (currentUserId != null) {
                        doc.reference.collection(SAVES).document(currentUserId).get().await().exists()
                    } else false
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    HomePost(
                        id = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "Unknown",
                        authorPhotoUrl = doc.getString("authorPhotoUrl")?.takeIf { it.isNotBlank() },
                        timeAgo = TimeUtils.timeAgo(createdAt),
                        imageUrl = doc.getString("imageUrl") ?: "",
                        location = doc.getString("location") ?: "",
                        rating = (doc.getDouble("rating") ?: 0.0).toFloat(),
                        title = doc.getString("title") ?: "",
                        caption = doc.getString("caption") ?: "",
                        hashtags = (doc.get("hashtags") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList(),
                        likeCount = (doc.getLong("likeCount") ?: 0L).toInt(),
                        commentCount = (doc.getLong("commentCount") ?: 0L).toInt(),
                        isLiked = isLiked,
                        isSaved = isSaved
                    )
                }
            }.map { it.await() }
        }
    }

    suspend fun toggleLike(post: HomePost) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(POSTS).document(post.id)
        val likeRef = postRef.collection(LIKES).document(userId)
        if (post.isLiked) {
            firestore.runTransaction { t ->
                t.delete(likeRef)
                t.update(postRef, "likeCount", FieldValue.increment(-1))
            }.await()
        } else {
            firestore.runTransaction { t ->
                t.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                t.update(postRef, "likeCount", FieldValue.increment(1))
            }.await()
        }
    }

    suspend fun toggleSave(post: HomePost) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(POSTS).document(post.id)
        val saveRef = postRef.collection(SAVES).document(userId)
        if (post.isSaved) {
            saveRef.delete().await()
        } else {
            saveRef.set(mapOf("createdAt" to FieldValue.serverTimestamp())).await()
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        val snapshot = firestore.collection(POSTS).document(postId)
            .collection(COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            Comment(
                id = doc.id,
                authorId = doc.getString("authorId") ?: "",
                userName = doc.getString("authorName") ?: "Anonymous",
                authorPhotoUrl = doc.getString("authorPhotoUrl")?.takeIf { it.isNotBlank() },
                text = doc.getString("text") ?: return@mapNotNull null,
                timeAgo = TimeUtils.timeAgo(createdAt)
            )
        }
    }

    suspend fun addComment(postId: String, text: String): Comment {
        val user = auth.currentUser ?: error("Not authenticated")
        val postRef = firestore.collection(POSTS).document(postId)
        val commentRef = postRef.collection(COMMENTS).document()
        firestore.runTransaction { t ->
            t.set(commentRef, hashMapOf(
                "authorId" to user.uid,
                "authorName" to (user.displayName ?: "Anonymous"),
                "authorPhotoUrl" to (user.photoUrl?.toString() ?: ""),
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            t.update(postRef, "commentCount", FieldValue.increment(1))
        }.await()
        return Comment(
            id = commentRef.id,
            authorId = user.uid,
            userName = user.displayName ?: "Anonymous",
            authorPhotoUrl = user.photoUrl?.toString()?.takeIf { it.isNotBlank() },
            text = text,
            timeAgo = "Just now"
        )
    }
}
