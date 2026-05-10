package com.bplayer

import android.app.Application
import com.bplayer.data.Library
import com.bplayer.data.Settings
import com.bplayer.data.bookmarks.BookmarkDb
import com.bplayer.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BPlayerApp : Application() {

    val settings: Settings by lazy { Settings(this) }
    val library: Library by lazy { Library(this) }
    val bookmarkDao by lazy { BookmarkDb.get(this).bookmarks() }
    val playback: PlaybackController by lazy { PlaybackController(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { playback.connect() }
    }
}
