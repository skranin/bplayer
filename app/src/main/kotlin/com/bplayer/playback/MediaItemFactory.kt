package com.bplayer.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.bplayer.data.BrowseEntry
import com.bplayer.data.TrackFile

private const val EXTRA_BOOK_KEY = "bplayer.bookKey"
private const val EXTRA_BOOK_NAME = "bplayer.bookName"

object MediaItemFactory {

    fun root(title: String): MediaItem = MediaItem.Builder()
        .setMediaId(MediaIds.ROOT)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .build()

    fun group(entry: BrowseEntry.Group): MediaItem = MediaItem.Builder()
        .setMediaId(MediaIds.groupId(entry.uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(entry.name)
                .setArtworkUri(entry.coverUri)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .build()

    fun book(entry: BrowseEntry.Book): MediaItem = MediaItem.Builder()
        .setMediaId(MediaIds.bookId(entry.uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(entry.name)
                .setArtworkUri(entry.coverUri)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
                .build()
        )
        .build()

    fun singleFileBook(entry: BrowseEntry.SingleFileBook): MediaItem {
        val extras = Bundle().apply {
            putString(EXTRA_BOOK_KEY, MediaIds.singleId(entry.uri))
            putString(EXTRA_BOOK_NAME, entry.name)
        }
        return MediaItem.Builder()
            .setMediaId(MediaIds.singleId(entry.uri))
            .setUri(entry.uri)
            .setMimeType(entry.mimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(entry.name.substringBeforeLast('.'))
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun track(
        track: TrackFile,
        bookFolderUri: Uri,
        bookName: String,
        coverUri: Uri?,
    ): MediaItem {
        val extras = Bundle().apply {
            putString(EXTRA_BOOK_KEY, MediaIds.bookId(bookFolderUri))
            putString(EXTRA_BOOK_NAME, bookName)
        }
        return MediaItem.Builder()
            .setMediaId(MediaIds.trackId(track.uri))
            .setUri(track.uri)
            .setMimeType(track.mimeType)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.name.substringBeforeLast('.'))
                    .setAlbumTitle(bookName)
                    .setArtworkUri(coverUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun bookKeyOf(item: MediaItem): String? =
        item.mediaMetadata.extras?.getString(EXTRA_BOOK_KEY)
}
