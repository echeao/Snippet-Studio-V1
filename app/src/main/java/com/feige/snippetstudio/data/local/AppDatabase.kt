package com.feige.snippetstudio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SnippetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao

    companion object {
        fun create(ctx: Context): AppDatabase = Room.databaseBuilder(
            ctx.applicationContext,
            AppDatabase::class.java,
            "snippet_studio.db"
        ).fallbackToDestructiveMigration().build()
    }
}
