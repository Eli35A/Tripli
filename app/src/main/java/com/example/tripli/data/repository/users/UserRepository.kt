package com.example.tripli.data.repository.users

import android.util.Log
import com.example.tripli.dao.AppUserDao
import com.example.tripli.dao.AppUserEntity
import com.example.tripli.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: AppUserDao
) {

    fun getCurrentUser(): AppUser? = auth.currentUser?.toAppUser()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getCachedUser(uid: String): AppUser? = withContext(Dispatchers.IO) {
        userDao.getUserById(uid)?.toDomain()
    }

    suspend fun getCachedCurrentUser(): AppUser? {
        val uid = auth.currentUser?.uid ?: return null
        return getCachedUser(uid)
    }

    suspend fun signInWithGoogle(idToken: String): Result<AppUser> {
        return withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "signInWithGoogle: starting credential sign-in")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val firebaseUser = auth.signInWithCredential(credential).await().user
                    ?: error("Authentication succeeded but no user was returned.")

                Log.d(TAG, "signInWithGoogle: firebase auth success for uid=${firebaseUser.uid}")
                val appUser = firebaseUser.toAppUser()

                saveLocalUser(appUser)
                saveRemoteUser(appUser)
                Log.d(TAG, "signInWithGoogle: remote save complete")
                appUser
            }
        }
    }

    suspend fun syncSignedInUser(): AppUser? {
        return withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: return@withContext null
            val appUser = currentUser.toAppUser()
            saveLocalUser(appUser)
            saveRemoteUser(appUser)
            appUser
        }
    }

    suspend fun updateProfile(uid: String, name: String, bio: String, localPhotoPath: String?) {
        withContext(Dispatchers.IO) {
            val existing = userDao.getUserById(uid) ?: return@withContext
            userDao.upsert(existing.copy(
                displayName = name,
                bio = bio,
                localPhotoPath = localPhotoPath ?: existing.localPhotoPath
            ))
            val updates = mutableMapOf<String, Any>("displayName" to name, "bio" to bio)
            firestore.collection(USERS_COLLECTION).document(uid).update(updates).await()
        }
    }

    suspend fun fetchAndCacheUser(uid: String): AppUser? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (!doc.exists()) return@runCatching null
            val existing = userDao.getUserById(uid)
            val entity = AppUserEntity(
                uid = uid,
                displayName = doc.getString("displayName") ?: existing?.displayName ?: "Traveler",
                email = doc.getString("email") ?: existing?.email,
                photoUrl = doc.getString("photoUrl") ?: existing?.photoUrl,
                localPhotoPath = existing?.localPhotoPath,
                bio = doc.getString("bio") ?: existing?.bio,
                lastLoginAt = existing?.lastLoginAt ?: System.currentTimeMillis()
            )
            userDao.upsert(entity)
            entity.toDomain()
        }.getOrNull()
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            auth.signOut()
            userDao.clearAll()
        }
    }

    private suspend fun saveRemoteUser(user: AppUser) {
        val payload = hashMapOf<String, Any?>(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "email" to user.email,
            "photoUrl" to user.photoUrl,
            "lastLoginAt" to FieldValue.serverTimestamp()
        )
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(payload, SetOptions.merge())
            .await()
    }

    private suspend fun saveLocalUser(user: AppUser) {
        // Preserve existing bio and localPhotoPath across sign-in refreshes
        val existing = userDao.getUserById(user.uid)
        userDao.upsert(
            AppUserEntity(
                uid = user.uid,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl,
                localPhotoPath = existing?.localPhotoPath,
                bio = existing?.bio,
                lastLoginAt = System.currentTimeMillis()
            )
        )
    }

    private fun FirebaseUser.toAppUser() = AppUser(
        uid = uid,
        displayName = displayName.orEmpty().ifBlank { "Traveler" },
        email = email,
        photoUrl = photoUrl?.toString()
    )

    companion object {
        private const val TAG = "UserRepository"
        private const val USERS_COLLECTION = "users"
    }
}
