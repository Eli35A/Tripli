package com.example.tripli.data.repository

import android.util.Log
import com.example.tripli.data.local.AppUserDao
import com.example.tripli.data.local.AppUserEntity
import com.example.tripli.data.model.AppUser
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

    fun getCurrentUser(): AppUser? {
        return auth.currentUser?.toAppUser()
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
                
                // Save locally
                Log.d(TAG, "signInWithGoogle: saving user locally")
                saveLocalUser(appUser)
                
                // Sync to remote and wait for confirm
                Log.d(TAG, "signInWithGoogle: saving user to remote firestore")
                saveRemoteUser(appUser)
                Log.d(TAG, "signInWithGoogle: remote save complete")
                
                appUser
            }
        }
    }

    suspend fun syncSignedInUser(): AppUser? {
        return withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser ?: return@withContext null
            Log.d(TAG, "syncSignedInUser: found current user ${currentUser.uid}, syncing...")
            val appUser = currentUser.toAppUser()
            saveLocalUser(appUser)
            saveRemoteUser(appUser)
            Log.d(TAG, "syncSignedInUser: sync complete")
            appUser
        }
    }

    suspend fun getCachedUser(uid: String): AppUser? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(uid)?.toDomain()
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "signOut: signing out user")
            auth.signOut()
            userDao.clearAll()
        }
    }

    /**
     * Saves user metadata to Firestore and waits for completion.
     */
    private suspend fun saveRemoteUser(user: AppUser) {
        val payload = hashMapOf<String, Any?>(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "email" to user.email,
            "photoUrl" to user.photoUrl,
            "lastLoginAt" to FieldValue.serverTimestamp()
        )

        Log.d(TAG, "saveRemoteUser: writing to firestore path users/${user.uid}")
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(payload, SetOptions.merge())
            .await()
        Log.d(TAG, "saveRemoteUser: firestore write finished")
    }

    private suspend fun saveLocalUser(user: AppUser) {
        userDao.upsert(
            AppUserEntity(
                uid = user.uid,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl,
                lastLoginAt = System.currentTimeMillis()
            )
        )
    }

    private fun FirebaseUser.toAppUser(): AppUser {
        return AppUser(
            uid = uid,
            displayName = displayName.orEmpty().ifBlank { "Traveler" },
            email = email,
            photoUrl = photoUrl?.toString()
        )
    }

    private fun AppUserEntity.toDomain(): AppUser {
        return AppUser(
            uid = uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl
        )
    }

    companion object {
        private const val TAG = "UserRepository"
        private const val USERS_COLLECTION = "users"
    }
}
