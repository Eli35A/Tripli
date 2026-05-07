package com.example.tripli.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HomePostDao {

    @Query("SELECT * FROM home_posts ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<HomePostEntity>

    @Query("SELECT * FROM home_posts ORDER BY createdAt DESC")
    suspend fun getAll(): List<HomePostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<HomePostEntity>)

    @Query("UPDATE home_posts SET localImagePath = :path WHERE id = :id")
    suspend fun updateLocalImagePath(id: String, path: String)

    @Query("SELECT COUNT(*) FROM home_posts")
    suspend fun count(): Int

    @Query("DELETE FROM home_posts WHERE cachedAt < :threshold")
    suspend fun evictOldEntries(threshold: Long)
}
