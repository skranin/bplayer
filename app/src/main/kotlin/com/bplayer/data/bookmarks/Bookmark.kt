package com.bplayer.data.bookmarks

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One bookmark per book.
 *
 * Key is the book's folder URI (or the file URI for a single-file book like an `.m4b` directly
 * under the root). Stores which file the user was on and the position within it. When the user
 * comes back to a book, we look up by [bookKey] and seek there.
 */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val bookKey: String,
    val currentFileUri: String,
    val positionMs: Long,
    val updatedAt: Long,
)
