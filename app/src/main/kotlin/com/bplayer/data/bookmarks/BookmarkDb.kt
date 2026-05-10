package com.bplayer.data.bookmarks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Bookmark::class], version = 1, exportSchema = true)
abstract class BookmarkDb : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao

    companion object {
        @Volatile private var instance: BookmarkDb? = null
        fun get(context: Context): BookmarkDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDb::class.java,
                    "bplayer.db",
                ).build().also { instance = it }
            }
    }
}
