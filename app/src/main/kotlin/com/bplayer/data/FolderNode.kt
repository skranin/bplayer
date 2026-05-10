package com.bplayer.data

import android.net.Uri

/** A child of the currently-displayed folder. */
sealed interface BrowseEntry {
    val uri: Uri
    val name: String

    /** Folder that contains audio files at top level → tap to play. */
    data class Book(
        override val uri: Uri,
        override val name: String,
        val coverUri: Uri?,
        val trackCount: Int,
    ) : BrowseEntry

    /** Folder with no direct audio, only subfolders → tap to drill in. */
    data class Group(
        override val uri: Uri,
        override val name: String,
        val coverUri: Uri?,
    ) : BrowseEntry

    /** Single audio file directly under the parent (e.g. a `.m4b` at the top level). */
    data class SingleFileBook(
        override val uri: Uri,
        override val name: String,
        val mimeType: String,
    ) : BrowseEntry
}

/** A child file inside a Book — used to build the playback queue. */
data class TrackFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
)

/** Bookmark key matching MediaIds prefixes. Null for Groups (no progress to track). */
val BrowseEntry.bookKey: String?
    get() = when (this) {
        is BrowseEntry.Book -> "B:$uri"
        is BrowseEntry.SingleFileBook -> "S:$uri"
        is BrowseEntry.Group -> null
    }
