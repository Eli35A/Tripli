package com.example.tripli.di

import android.content.Context
import com.example.tripli.data.local.AppDatabase
import com.example.tripli.data.repository.FeaturedPostsRepository
import com.example.tripli.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object ServiceLocator {

    @Volatile
    private var database: AppDatabase? = null

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
}
