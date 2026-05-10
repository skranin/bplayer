package com.bplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * Single owner of the bound [MediaController]. UI observes [state] and calls intent methods.
 *
 * Lifecycle: [connect] is called once from [com.bplayer.BPlayerApp]; [release] is unused — the
 * controller lives for the process lifetime. Reconnecting on activity recreate is unnecessary
 * because the underlying session lives in the bound service.
 */
class PlaybackController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionTickJob: Job? = null

    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(
            context,
            ComponentName(context, PlayerService::class.java),
        )
        val ctl = MediaController.Builder(context, token).buildAsync().await()
        controller = ctl
        ctl.addListener(playerListener)
        emitState()
    }

    fun playBookFolder(folderUri: Uri, title: String, coverUri: Uri?) {
        play(buildBookSeed(folderUri, title, coverUri))
    }

    fun playSingleFile(fileUri: Uri, title: String) {
        play(buildSingleSeed(fileUri, title))
    }

    private fun play(seed: MediaItem) {
        val c = controller ?: return
        c.setMediaItems(listOf(seed))
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekToNext() {
        controller?.seekToNextMediaItem()
    }

    fun seekToPrevious() {
        val c = controller ?: return
        // If we're more than 3s into a track, restart it; otherwise go to previous.
        if (c.currentPosition > 3_000) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /**
     * If the currently-loaded queue belongs to [bookKey], rewind to the start of the first
     * track and resume playing. No-op if the book isn't loaded or isn't playing.
     */
    fun resetIfCurrent(bookKey: String) {
        val c = controller ?: return
        val item = c.currentMediaItem ?: return
        val currentKey = MediaItemFactory.bookKeyOf(item) ?: return
        if (currentKey != bookKey) return
        c.seekTo(0, 0L)
        c.play()
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            emitState()
            if (player.isPlaying && positionTickJob?.isActive != true) startPositionTicks()
            else if (!player.isPlaying) stopPositionTicks()
        }
    }

    private fun startPositionTicks() {
        positionTickJob = scope.launch {
            while (true) {
                delay(500)
                emitPositionOnly()
            }
        }
    }

    private fun stopPositionTicks() {
        positionTickJob?.cancel()
        positionTickJob = null
    }

    private fun emitState() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.value = PlaybackUiState(
            connected = true,
            isPlaying = c.isPlaying,
            title = item?.mediaMetadata?.title?.toString() ?: "",
            albumTitle = item?.mediaMetadata?.albumTitle?.toString() ?: "",
            artworkUri = item?.mediaMetadata?.artworkUri,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0 } ?: 0L,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
        )
    }

    private fun emitPositionOnly() {
        val c = controller ?: return
        _state.value = _state.value.copy(
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0 } ?: _state.value.durationMs,
        )
    }

    private fun buildBookSeed(folderUri: Uri, title: String, coverUri: Uri?): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaIds.bookId(folderUri))
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtworkUri(coverUri)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    private fun buildSingleSeed(fileUri: Uri, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaIds.singleId(fileUri))
            .setUri(fileUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    fun release() {
        positionTickJob?.cancel()
        controller?.release()
        controller = null
        scope.cancel()
    }
}

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val albumTitle: String = "",
    val artworkUri: Uri? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)
