package com.bplayer.data.bookmarks

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Upsert
    suspend fun upsert(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE bookKey = :key LIMIT 1")
    suspend fun get(key: String): Bookmark?

    @Query("SELECT * FROM bookmarks ORDER BY updatedAt DESC LIMIT 1")
    suspend fun mostRecent(): Bookmark?

    @Query("SELECT * FROM bookmarks ORDER BY updatedAt DESC")
    fun all(): Flow<List<Bookmark>>

    @Query("DELETE FROM bookmarks WHERE bookKey = :key")
    suspend fun delete(key: String)
}
