@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bplayer.ui.browser

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.bplayer.BPlayerApp
import com.bplayer.R
import com.bplayer.data.BrowseEntry
import com.bplayer.ui.player.PlayerPane
import com.bplayer.util.releasePersistablePermission
import com.bplayer.util.takePersistablePermission
import kotlinx.coroutines.launch

private data class StackEntry(val uri: Uri, val title: String)

@Composable
fun BrowserRoute(
    rootUri: Uri,
    onPlaybackStarted: () -> Unit,
    onMiniPlayerTap: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = remember(ctx) { ctx.applicationContext as BPlayerApp }

    val rootTitle = stringResource(R.string.library_root)
    val stack = remember(rootUri) { mutableStateListOf(StackEntry(rootUri, rootTitle)) }
    val current = stack.last()

    val vm: BrowserViewModel = viewModel()
    LaunchedEffect(current.uri) { vm.load(current.uri, current.title) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    val state by vm.state.collectAsStateWithLifecycle()
    val playbackState by app.playback.state.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val rootPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { newUri ->
        if (newUri != null) {
            ctx.contentResolver.takePersistablePermission(newUri)
            if (newUri != rootUri) {
                ctx.contentResolver.releasePersistablePermission(rootUri)
            }
            coroutineScope.launch { app.settings.setRootUri(newUri.toString()) }
        }
    }

    LaunchedEffect(state.permissionLost) {
        if (state.permissionLost) app.settings.setRootUri(null)
    }

    BackHandler(enabled = stack.size > 1) {
        stack.removeAt(stack.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (stack.size > 1) {
                        IconButton(onClick = { stack.removeAt(stack.lastIndex) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (stack.size == 1) {
                        IconButton(onClick = { rootPicker.launch(rootUri) }) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = stringResource(R.string.change_root),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (playbackState.connected && playbackState.title.isNotEmpty()) {
                PlayerPane(
                    state = playbackState,
                    onPlayPause = { app.playback.togglePlayPause() },
                    onNext = { app.playback.seekToNext() },
                    onPrevious = { app.playback.seekToPrevious() },
                    onSeek = { app.playback.seekTo(it) },
                    onExpand = onMiniPlayerTap,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        var pendingDelete by remember { mutableStateOf<BrowseEntry?>(null) }
        when {
            state.isLoading && state.entries.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center,
            ) { CircularProgressIndicator() }
            state.entries.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.empty_folder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { vm.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                EntryList(
                    entries = state.entries,
                    contentPadding = padding,
                    onEntryClick = { entry ->
                        when (entry) {
                            is BrowseEntry.Group -> stack.add(StackEntry(entry.uri, entry.name))
                            is BrowseEntry.Book -> {
                                app.playback.playBookFolder(
                                    folderUri = entry.uri,
                                    title = entry.name,
                                    coverUri = entry.coverUri,
                                )
                                onPlaybackStarted()
                            }
                            is BrowseEntry.SingleFileBook -> {
                                app.playback.playSingleFile(
                                    fileUri = entry.uri,
                                    title = entry.name.substringBeforeLast('.'),
                                )
                                onPlaybackStarted()
                            }
                        }
                    },
                    onResetClick = { vm.resetProgress(it) },
                    onDeleteClick = { pendingDelete = it },
                )
            }
        }
        pendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.confirm_delete_title)) },
                text = { Text(stringResource(R.string.confirm_delete_body, target.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.delete(target) { /* refresh handled in VM */ }
                        pendingDelete = null
                    }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun EntryList(
    entries: List<BrowseEntry>,
    contentPadding: PaddingValues,
    onEntryClick: (BrowseEntry) -> Unit,
    onResetClick: (BrowseEntry) -> Unit,
    onDeleteClick: (BrowseEntry) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(entries, key = { it.uri.toString() }) { entry ->
            EntryRow(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onResetClick = { onResetClick(entry) },
                onDeleteClick = { onDeleteClick(entry) },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: BrowseEntry,
    onClick: () -> Unit,
    onResetClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val cover = when (entry) {
        is BrowseEntry.Book -> entry.coverUri
        is BrowseEntry.Group -> entry.coverUri
        is BrowseEntry.SingleFileBook -> null
    }
    val (icon, supporting) = when (entry) {
        is BrowseEntry.Group -> Icons.Default.Folder to null
        is BrowseEntry.Book -> Icons.Default.PlayArrow to "${entry.trackCount} files"
        is BrowseEntry.SingleFileBook -> Icons.Default.AudioFile to null
    }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = entry.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = supporting?.let {
            { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.End) {
                if (entry !is BrowseEntry.Group) {
                    IconButton(onClick = onResetClick) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = stringResource(R.string.reset_progress),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
