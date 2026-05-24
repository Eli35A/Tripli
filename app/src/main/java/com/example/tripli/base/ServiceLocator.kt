package com.example.tripli.base

import android.content.Context
import com.example.tripli.dao.AppDatabase
import com.example.tripli.dao.ImageCacheManager
import com.example.tripli.data.remote.CloudinaryUploader
import com.example.tripli.data.repository.featured.FeaturedPostsRepository
import com.example.tripli.data.repository.posts.HomePostRepository
import com.example.tripli.data.repository.users.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object ServiceLocator {

    @Volatile
    private var database: AppDatabase? = null

    val cloudinaryUploader = CloudinaryUploader()

    private fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: AppDatabase.build(context.applicationContext).also {
                database = it
            }
        }
    }

    fun provideUserRepository(context: Context): UserRepository {
        val database = getDatabase(context)
        return UserRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
            userDao = database.appUserDao()
        )
    }

    fun provideFeaturedPostsRepository(): FeaturedPostsRepository {
        return FeaturedPostsRepository()
    }

    fun provideHomePostRepository(context: Context): HomePostRepository {
        val database = getDatabase(context)
        val postDao = database.homePostDao()
        return HomePostRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            postDao = postDao,
            userDao = database.appUserDao(),
            imageCacheManager = ImageCacheManager(context.applicationContext, postDao),
            cloudinaryUploader = cloudinaryUploader
        )
    }
}
