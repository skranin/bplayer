package com.bplayer.playback

import android.net.Uri
import androidx.core.net.toUri

/** mediaId scheme for our browse tree and play queue. */
object MediaIds {
    const val ROOT = "ROOT"
    private const val PFX_GROUP = "G:"
    private const val PFX_BOOK = "B:"
    private const val PFX_SINGLE = "S:"
    private const val PFX_TRACK = "T:"

    fun groupId(uri: Uri) = "$PFX_GROUP$uri"
    fun bookId(uri: Uri) = "$PFX_BOOK$uri"
    fun singleId(uri: Uri) = "$PFX_SINGLE$uri"
    fun trackId(uri: Uri) = "$PFX_TRACK$uri"

    fun isGroup(id: String) = id.startsWith(PFX_GROUP)
    fun isBook(id: String) = id.startsWith(PFX_BOOK)
    fun isSingle(id: String) = id.startsWith(PFX_SINGLE)
    fun isTrack(id: String) = id.startsWith(PFX_TRACK)

    fun uriOf(id: String): Uri = id.substringAfter(':').toUri()
}
