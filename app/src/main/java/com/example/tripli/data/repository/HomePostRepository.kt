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
import com.google.firebase.firestore.FieldPath
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
        private const val USER_LIKES = "userLikes"
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

    suspend fun getCachedPosts(): List<HomePost> =
        postDao.getAll().map { it.toModel() }

    suspend fun getCachedUserPosts(userId: String): List<HomePost> =
        postDao.getPostsByAuthor(userId).map { it.toModel() }

    suspend fun getCachedLikedPosts(): List<HomePost> =
        postDao.getLikedPosts().map { it.toModel() }

    suspend fun getFirstPage(): List<HomePost> {
        lastDocument = null
        hasMore = true
        return fetchPage(cursor = null)
    }

    suspend fun getNextPage(): List<HomePost> {
        if (!hasMore) return emptyList()
        return fetchPage(cursor = lastDocument)
    }

    suspend fun getAllCachedOrFirstPage(): List<HomePost> {
        val cached = postDao.getAll()
        return if (cached.isNotEmpty()) cached.map { it.toModel() } else getFirstPage()
    }

    suspend fun getUserPosts(userId: String): List<HomePost> {
        val cached = postDao.getPostsByAuthor(userId).map { it.toModel() }
        return try {
            val snapshot = firestore.collection(POSTS)
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val posts = snapshot.documents.map { it.toHomePost() }
            postDao.insertAll(snapshot.documents.map { it.toEntity() })
            posts.filter { imageCacheManager.localPathFor(it.id) == null }
                .forEach { imageCacheManager.scheduleCache(it.id, it.imageUrl) }
            posts
        } catch (e: Exception) {
            cached
        }
    }

    suspend fun getLikedPosts(userId: String): List<HomePost> {
        val cached = postDao.getLikedPosts().map { it.toModel() }
        return try {
            val likedDocs = firestore.collection(USER_LIKES)
                .document(userId)
                .collection(POSTS)
                .get().await()
            val postIds = likedDocs.documents.map { it.id }.filter { it.isNotBlank() }
            if (postIds.isEmpty()) return cached

            val posts = mutableListOf<HomePost>()
            postIds.chunked(10).forEach { batch ->
                firestore.collection(POSTS)
                    .whereIn(FieldPath.documentId(), batch)
                    .get().await()
                    .documents
                    .forEach { posts.add(it.toHomePost(isLiked = true)) }
            }
            postDao.insertAll(posts.map { p ->
                postDao.getAll().find { it.id == p.id }
                    ?.copy(isLiked = true, cachedAt = System.currentTimeMillis())
                    ?: p.toEntityLiked()
            })
            posts
        } catch (e: Exception) {
            cached
        }
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
                async {
                    val isLiked = if (currentUserId != null)
                        doc.reference.collection(LIKES).document(currentUserId).get().await().exists()
                    else false
                    val isSaved = if (currentUserId != null)
                        doc.reference.collection(SAVES).document(currentUserId).get().await().exists()
                    else false
                    doc.toHomePost(isLiked, isSaved)
                }
            }.map { it.await() }
        }

        postDao.insertAll(snapshot.documents.mapIndexed { i, doc ->
            doc.toEntity(posts[i].isLiked, posts[i].isSaved)
        })
        posts.filter { imageCacheManager.localPathFor(it.id) == null }
            .forEach { imageCacheManager.scheduleCache(it.id, it.imageUrl) }
        return posts
    }

    // Private extension functions to reduce duplication
    private fun DocumentSnapshot.toHomePost(isLiked: Boolean = false, isSaved: Boolean = false): HomePost {
        val createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        return HomePost(
            id = id,
            authorId = getString("authorId") ?: "",
            authorName = getString("authorName") ?: "Unknown",
            authorPhotoUrl = getString("authorPhotoUrl")?.takeIf { it.isNotBlank() },
            timeAgo = TimeUtils.timeAgo(createdAt),
            imageUrl = getString("imageUrl") ?: "",
            localImagePath = imageCacheManager.localPathFor(id),
            location = getString("location") ?: "",
            rating = (getDouble("rating") ?: 0.0).toFloat(),
            title = getString("title") ?: "",
            caption = getString("caption") ?: "",
            hashtags = (get("hashtags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            likeCount = (getLong("likeCount") ?: 0L).toInt(),
            commentCount = (getLong("commentCount") ?: 0L).toInt(),
            isLiked = isLiked,
            isSaved = isSaved
        )
    }

    private fun DocumentSnapshot.toEntity(isLiked: Boolean = false, isSaved: Boolean = false): HomePostEntity {
        val createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        return HomePostEntity(
            id = id,
            authorId = getString("authorId") ?: "",
            authorName = getString("authorName") ?: "Unknown",
            authorPhotoUrl = getString("authorPhotoUrl")?.takeIf { it.isNotBlank() },
            createdAt = createdAt,
            imageUrl = getString("imageUrl") ?: "",
            localImagePath = imageCacheManager.localPathFor(id),
            location = getString("location") ?: "",
            rating = (getDouble("rating") ?: 0.0).toFloat(),
            title = getString("title") ?: "",
            caption = getString("caption") ?: "",
            hashtags = (get("hashtags") as? List<*>)?.mapNotNull { it as? String }?.joinToString("|") ?: "",
            likeCount = (getLong("likeCount") ?: 0L).toInt(),
            commentCount = (getLong("commentCount") ?: 0L).toInt(),
            isLiked = isLiked,
            isSaved = isSaved,
            cachedAt = System.currentTimeMillis()
        )
    }

    private fun HomePost.toEntityLiked() = HomePostEntity(
        id = id, authorId = authorId, authorName = authorName,
        authorPhotoUrl = authorPhotoUrl, createdAt = 0L,
        imageUrl = imageUrl, localImagePath = localImagePath,
        location = location, rating = rating, title = title,
        caption = caption, hashtags = hashtags.joinToString("|"),
        likeCount = likeCount, commentCount = commentCount,
        isLiked = true, isSaved = isSaved,
        cachedAt = System.currentTimeMillis()
    )

    suspend fun toggleLike(post: HomePost) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(POSTS).document(post.id)
        val likeRef = postRef.collection(LIKES).document(userId)
        val userLikeRef = firestore.collection(USER_LIKES).document(userId)
            .collection(POSTS).document(post.id)
        if (post.isLiked) {
            firestore.runTransaction { t ->
                t.delete(likeRef)
                t.delete(userLikeRef)
                t.update(postRef, "likeCount", FieldValue.increment(-1))
            }.await()
        } else {
            firestore.runTransaction { t ->
                t.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                t.set(userLikeRef, mapOf("likedAt" to FieldValue.serverTimestamp(), "postId" to post.id))
                t.update(postRef, "likeCount", FieldValue.increment(1))
            }.await()
        }
    }

    suspend fun toggleSave(post: HomePost) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(POSTS).document(post.id)
        val saveRef = postRef.collection(SAVES).document(userId)
        if (post.isSaved) saveRef.delete().await()
        else saveRef.set(mapOf("createdAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun getComments(postId: String): List<Comment> {
        val snapshot = firestore.collection(POSTS).document(postId)
            .collection(COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get().await()
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

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun deletePost(postId: String) {
        firestore.collection(POSTS).document(postId).delete().await()
        postDao.deleteById(postId)
    }

    suspend fun updatePost(postId: String, imageBytes: ByteArray?, existingImageUrl: String, location: String, rating: Float, caption: String, hashtags: List<String>) {
        val finalImageUrl = if (imageBytes != null)
            "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        else existingImageUrl

        val updates = mutableMapOf<String, Any>(
            "location" to location,
            "title" to location,
            "rating" to rating.toDouble(),
            "caption" to caption,
            "hashtags" to hashtags
        )
        if (imageBytes != null) updates["imageUrl"] = finalImageUrl
        firestore.collection(POSTS).document(postId).update(updates).await()
        postDao.updatePost(postId, location, rating, caption, hashtags.joinToString("|"), finalImageUrl)
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
