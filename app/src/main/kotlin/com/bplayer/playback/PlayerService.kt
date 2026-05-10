package com.bplayer.playback

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.bplayer.R
import com.bplayer.data.Library
import com.bplayer.data.Settings
import com.bplayer.data.bookmarks.Bookmark
import com.bplayer.data.bookmarks.BookmarkDao
import com.bplayer.data.bookmarks.BookmarkDb
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var library: Library
    private lateinit var settings: Settings
    private lateinit var bookmarkDao: BookmarkDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tickJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        library = Library(this)
        settings = Settings(this)
        bookmarkDao = BookmarkDb.get(this).bookmarks()

        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ true)
            .build()
        player.addListener(playerListener)

        session = MediaLibrarySession.Builder(this, player, librarySessionCallback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveBookmarkBlocking()
        scope.cancel()
        player.removeListener(playerListener)
        session.release()
        player.release()
        super.onDestroy()
    }

    // ---- Browse tree (also used by Android Auto) -----------------------------------------

    private val librarySessionCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItemFactory.root(getString(R.string.library_root)),
                params,
            )
        )

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            val item = when {
                mediaId == MediaIds.ROOT ->
                    MediaItemFactory.root(getString(R.string.library_root))
                MediaIds.isGroup(mediaId) -> {
                    val uri = MediaIds.uriOf(mediaId)
                    val children = library.listChildren(uri)
                    MediaItemFactory.group(
                        com.bplayer.data.BrowseEntry.Group(
                            uri = uri,
                            name = library.displayName(uri).ifEmpty { uri.lastPathSegment ?: "" },
                            coverUri = library.pickCover(children),
                        )
                    )
                }
                MediaIds.isBook(mediaId) -> {
                    val uri = MediaIds.uriOf(mediaId)
                    val children = library.listChildren(uri)
                    MediaItemFactory.book(
                        com.bplayer.data.BrowseEntry.Book(
                            uri = uri,
                            name = library.displayName(uri).ifEmpty { uri.lastPathSegment ?: "" },
                            coverUri = library.pickCover(children),
                            trackCount = children.count { it.isAudio },
                        )
                    )
                }
                MediaIds.isSingle(mediaId) -> {
                    val uri = MediaIds.uriOf(mediaId)
                    MediaItemFactory.singleFileBook(
                        com.bplayer.data.BrowseEntry.SingleFileBook(
                            uri = uri,
                            name = library.displayName(uri).ifEmpty { uri.lastPathSegment ?: "" },
                            mimeType = "audio/*",
                        )
                    )
                }
                else -> MediaItemFactory.root(getString(R.string.library_root))
            }
            LibraryResult.ofItem(item, /* params = */ null)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val rootStr = settings.rootUri.first()
                ?: return@future LibraryResult.ofItemList(ImmutableList.of(), params)
            val folderUri = if (parentId == MediaIds.ROOT) rootStr.toUri()
            else MediaIds.uriOf(parentId)
            val items = library.browse(folderUri).map { entry ->
                when (entry) {
                    is com.bplayer.data.BrowseEntry.Group -> MediaItemFactory.group(entry)
                    is com.bplayer.data.BrowseEntry.Book -> MediaItemFactory.book(entry)
                    is com.bplayer.data.BrowseEntry.SingleFileBook ->
                        MediaItemFactory.singleFileBook(entry)
                }
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }

        // ---- Play requests ---------------------------------------------------------------

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Save the OUTGOING book's position synchronously *before* replacing items,
            // otherwise player.currentMediaItem will already be the new seed by the time
            // listeners fire.
            saveBookmarkBeforeSwap()
            return scope.future {
                resolveForPlayback(mediaItems, startIndex, startPositionMs)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = scope.future {
            mediaItems.flatMap { expandIfNeeded(it) }
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val recent = bookmarkDao.mostRecent()
                ?: return@future MediaSession.MediaItemsWithStartPosition(
                    emptyList(), 0, 0L,
                )
            val key = recent.bookKey
            val seedItem = when {
                MediaIds.isBook(key) -> MediaItem.Builder().setMediaId(key).build()
                MediaIds.isSingle(key) -> MediaItem.Builder()
                    .setMediaId(key)
                    .setUri(MediaIds.uriOf(key))
                    .build()
                else -> return@future MediaSession.MediaItemsWithStartPosition(
                    emptyList(), 0, 0L,
                )
            }
            resolveForPlayback(listOf(seedItem), C.INDEX_UNSET, C.TIME_UNSET)
        }
    }

    /** Expand a "book" mediaId into a list of track MediaItems; rebuild single/track items
     *  through MediaItemFactory so the bookKey extra is always set. */
    private suspend fun expandIfNeeded(item: MediaItem): List<MediaItem> {
        val id = item.mediaId
        return when {
            MediaIds.isBook(id) -> {
                val folderUri = MediaIds.uriOf(id)
                val tracks = library.audioTracks(folderUri)
                if (tracks.isEmpty()) return emptyList()
                val children = library.listChildren(folderUri)
                val coverUri = library.pickCover(children)
                val bookName = item.mediaMetadata.title?.toString()?.takeIf { it.isNotEmpty() }
                    ?: library.displayName(folderUri).ifEmpty { folderUri.lastPathSegment ?: "" }
                tracks.map { MediaItemFactory.track(it, folderUri, bookName, coverUri) }
            }
            MediaIds.isSingle(id) -> {
                val uri = MediaIds.uriOf(id)
                val name = item.mediaMetadata.title?.toString()?.takeIf { it.isNotEmpty() }
                    ?: library.displayName(uri).ifEmpty { uri.lastPathSegment ?: "" }
                val mime = item.localConfiguration?.mimeType ?: "audio/*"
                listOf(
                    MediaItemFactory.singleFileBook(
                        com.bplayer.data.BrowseEntry.SingleFileBook(uri, name, mime),
                    )
                )
            }
            MediaIds.isTrack(id) -> listOf(ensurePlayable(item, id))
            else -> emptyList()
        }
    }

    /** Some flows (Auto resumption) hand us an item with no localConfiguration; fix that. */
    private fun ensurePlayable(item: MediaItem, id: String): MediaItem {
        if (item.localConfiguration != null) return item
        return item.buildUpon().setUri(MediaIds.uriOf(id)).build()
    }

    private suspend fun resolveForPlayback(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): MediaSession.MediaItemsWithStartPosition {
        val resolved = mediaItems.flatMap { expandIfNeeded(it) }
        if (resolved.isEmpty()) {
            return MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
        }
        val seed = mediaItems.firstOrNull()
        val seedId = seed?.mediaId
        val bookKey: String? = if (
            mediaItems.size == 1 && seedId != null &&
            (MediaIds.isBook(seedId) || MediaIds.isSingle(seedId))
        ) seedId else null
        if (bookKey != null) {
            val bm = bookmarkDao.get(bookKey)
            if (bm != null) {
                val idx = resolved.indexOfFirst {
                    it.localConfiguration?.uri.toString() == bm.currentFileUri
                }.coerceAtLeast(0)
                return MediaSession.MediaItemsWithStartPosition(
                    resolved, idx, bm.positionMs,
                )
            }
        }
        val effectiveIdx = if (startIndex == C.INDEX_UNSET) 0 else startIndex
        val effectivePos = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs
        return MediaSession.MediaItemsWithStartPosition(resolved, effectiveIdx, effectivePos)
    }

    // ---- Bookmarks ------------------------------------------------------------------------

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            saveBookmarkAsync()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startTicks() else {
                stopTicks()
                saveBookmarkAsync()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) saveBookmarkAsync()
        }
    }

    private fun startTicks() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (true) {
                delay(10_000)
                withContext(Dispatchers.Main) { saveBookmarkSnapshot() }?.let { snap ->
                    bookmarkDao.upsert(snap)
                }
            }
        }
    }

    private fun stopTicks() {
        tickJob?.cancel()
        tickJob = null
    }

    /** Take a bookmark snapshot from the player on the main thread. */
    private fun saveBookmarkSnapshot(): Bookmark? {
        val item = player.currentMediaItem ?: return null
        val bookKey = MediaItemFactory.bookKeyOf(item) ?: return null
        val fileUri = item.localConfiguration?.uri ?: return null
        val pos = player.currentPosition.coerceAtLeast(0L)
        return Bookmark(
            bookKey = bookKey,
            currentFileUri = fileUri.toString(),
            positionMs = pos,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun saveBookmarkAsync() {
        val snap = saveBookmarkSnapshot() ?: return
        scope.launch { bookmarkDao.upsert(snap) }
    }

    // Snapshots+saves *before* the player swaps queues, blocking briefly on the IO write so
    // the outgoing book's position lands before the new queue replaces currentMediaItem.
    private fun saveBookmarkBeforeSwap() {
        val snap = saveBookmarkSnapshot() ?: return
        kotlinx.coroutines.runBlocking { bookmarkDao.upsert(snap) }
    }

    private fun saveBookmarkBlocking() {
        val snap = saveBookmarkSnapshot() ?: return
        kotlinx.coroutines.runBlocking { bookmarkDao.upsert(snap) }
    }
}
