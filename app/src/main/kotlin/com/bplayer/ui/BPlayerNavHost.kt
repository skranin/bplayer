package com.bplayer.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bplayer.BPlayerApp
import com.bplayer.data.toUriOrNull
import com.bplayer.ui.browser.BrowserRoute
import com.bplayer.ui.player.NowPlayingScreen
import com.bplayer.ui.root.RootPickerScreen

@Composable
fun BPlayerNavHost() {
    val ctx = LocalContext.current
    val app = remember(ctx) { ctx.applicationContext as BPlayerApp }
    val rootUri by app.settings.rootUri.collectAsStateWithLifecycle(initialValue = null)

    var showingNowPlaying by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        val parsed = rootUri?.toUriOrNull()
        if (parsed == null) {
            RootPickerScreen()
        } else {
            BrowserRoute(
                rootUri = parsed,
                onPlaybackStarted = { showingNowPlaying = true },
                onMiniPlayerTap = { showingNowPlaying = true },
            )
            if (showingNowPlaying) {
                NowPlayingScreen(onBack = { showingNowPlaying = false })
            }
        }
    }
}

internal fun Application.asBPlayer(): BPlayerApp = this as BPlayerApp
