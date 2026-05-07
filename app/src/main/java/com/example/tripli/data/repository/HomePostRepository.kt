package com.example.tripli.data.repository

import android.util.Base64
import com.example.tripli.data.local.HomePostDao
import com.example.tripli.data.local.HomePostEntity
import com.example.tripli.data.local.ImageCacheManager
import com.example.tripli.data.model.Comment
import com.example.tripli.data.model.HomePost
import com.example.tripli.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class HomePostRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postDao: HomePostDao,
    private val imageCacheManager: ImageCacheManager
) {

    companion object {
        private const val POSTS = "posts"
        private const val LIKES = "likes"
        private const val SAVES = "saves"
        private const val COMMENTS = "comments"
        const val PAGE_SIZE = 10L

        private val AVATAR_COLORS = listOf(
            "#F4A460", "#5BA4CF", "#6DBF8A",
            "#B97FF5", "#F47C7C", "#FFB347"
        )

        fun colorForId(id: String): String =
            AVATAR_COLORS[Math.abs(id.hashCode()) % AVATAR_COLORS.size]
    }

    private var lastDocument: DocumentSnapshot? = null
    private var hasMore = true

    fun hasMorePages(): Boolean = hasMore

    // Returns Room cache immediately — no network call
    suspend fun getCachedPosts(): List<HomePost> =
        postDao.getAll().map { it.toModel() }

    // Resets cursor and loads the first page from Firestore
    suspend fun getFirstPage(): List<HomePost> {
        lastDocument = null
        hasMore = true
        return fetchPage(cursor = null)
    }

    // Loads the next page using the last Firestore cursor
    suspend fun getNextPage(): List<HomePost> {
        if (!hasMore) return emptyList()
        return fetchPage(cursor = lastDocument)
    }

    // Used by MapViewModel: returns cache if available, otherwise fetches first page
    suspend fun getAllCachedOrFirstPage(): List<HomePost> {
        val cached = postDao.getAll()
        return if (cached.isNotEmpty()) cached.map { it.toModel() } else getFirstPage()
    }

    private suspend fun fetchPage(cursor: DocumentSnapshot?): List<HomePost> {
        val currentUserId = auth.currentUser?.uid

        var query = firestore.collection(POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
        if (cursor != null) query = query.startAfter(cursor)

        val snapshot = query.get().await()
        hasMore = snapshot.documents.size >= PAGE_SIZE
        lastDocument = snapshot.documents.lastOrNull()

        val posts = coroutineScope {
            snapshot.documents.map { doc ->
                async { buildPost(doc, currentUserId) }
            }.map { it.await() }
        }

        // Persist to Room, keeping any already-cached local image path
        val entities = snapshot.documents.mapIndexed { i, doc ->
            val post = posts[i]
            val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            HomePostEntity(
                id = post.id,
                authorId = post.authorId,
                authorName = post.authorName,
                authorPhotoUrl = post.authorPhotoUrl,
                createdAt = createdAt,
                imageUrl = post.imageUrl,
                localImagePath = imageCacheManager.localPathFor(post.id),
                location = post.location,
                rating = post.rating,
                title = post.title,
                caption = post.caption,
                hashtags = post.hashtags.joinToString("|"),
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                isLiked = post.isLiked,
                isSaved = post.isSaved,
                cachedAt = System.currentTimeMillis()
            )
        }
        postDao.insertAll(entities)

        // Kick off background image downloads for posts not yet cached
        posts.filter { imageCacheManager.localPathFor(it.id) == null }
            .forEach { imageCacheManager.scheduleCache(it.id, it.imageUrl) }

        return posts
    }

    private suspend fun buildPost(doc: DocumentSnapshot, currentUserId: String?): HomePost {
        val isLiked = if (currentUserId != null) {
            doc.reference.collection(LIKES).document(currentUserId).get().await().exists()
        } else false
        val isSaved = if (currentUserId != null) {
            doc.reference.collection(SAVES).document(currentUserId).get().await().exists()
        } else false
        val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
        return HomePost(
            id = doc.id,
            authorId = doc.getString("authorId") ?: "",
            authorName = doc.getString("authorName") ?: "Unknown",
            authorPhotoUrl = doc.getString("authorPhotoUrl")?.takeIf { it.isNotBlank() },
            timeAgo = TimeUtils.timeAgo(createdAt),
            imageUrl = doc.getString("imageUrl") ?: "",
            localImagePath = imageCacheManager.localPathFor(doc.id),
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

    suspend fun createPost(imageBytes: ByteArray?, location: String, rating: Float, caption: String, hashtags: List<String>) {
        val user = auth.currentUser ?: error("Not authenticated")

        val imageUrl = if (imageBytes != null) {
            "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } else ""

        firestore.collection(POSTS).add(
            mapOf(
                "authorId" to user.uid,
                "authorName" to (user.displayName ?: "Anonymous"),
                "authorPhotoUrl" to (user.photoUrl?.toString() ?: ""),
                "location" to location,
                "title" to location,
                "rating" to rating.toDouble(),
                "caption" to caption,
                "hashtags" to hashtags,
                "imageUrl" to imageUrl,
                "likeCount" to 0L,
                "commentCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
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
