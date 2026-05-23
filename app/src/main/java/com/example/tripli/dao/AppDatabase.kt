package com.example.tripli.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppUserEntity::class, HomePostEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appUserDao(): AppUserDao
    abstract fun homePostDao(): HomePostDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `home_posts` (
                        `id` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `authorPhotoUrl` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `localImagePath` TEXT,
                        `location` TEXT NOT NULL,
                        `rating` REAL NOT NULL,
                        `title` TEXT NOT NULL,
                        `caption` TEXT NOT NULL,
                        `hashtags` TEXT NOT NULL,
                        `likeCount` INTEGER NOT NULL,
                        `commentCount` INTEGER NOT NULL,
                        `isLiked` INTEGER NOT NULL DEFAULT 0,
                        `isSaved` INTEGER NOT NULL DEFAULT 0,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `localPhotoPath` TEXT")
                db.execSQL("ALTER TABLE `users` ADD COLUMN `bio` TEXT")
            }
        }

        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "tripli.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
