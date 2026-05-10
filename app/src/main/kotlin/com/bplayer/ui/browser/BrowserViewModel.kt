package com.bplayer.ui.browser

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bplayer.BPlayerApp
import com.bplayer.data.BrowseEntry
import com.bplayer.data.bookKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(app: Application) : AndroidViewModel(app) {

    private val app = app as BPlayerApp
    private val library = this.app.library
    private val bookmarks = this.app.bookmarkDao

    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    /** Re-scan the currently-open folder. No-op until [load] has set a current folder. */
    fun refresh() {
        val current = _state.value.currentFolder ?: return
        load(current, _state.value.title)
    }

    fun resetProgress(entry: BrowseEntry) {
        val key = entry.bookKey ?: return
        app.playback.resetIfCurrent(key)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { bookmarks.delete(key) }
        }
    }

    fun delete(entry: BrowseEntry, onDone: () -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { library.delete(entry.uri) }
            val current = _state.value.currentFolder
            if (ok && current != null) {
                val refreshed = withContext(Dispatchers.IO) { library.browse(current) }
                _state.value = _state.value.copy(entries = refreshed)
            }
            onDone()
        }
    }

    fun load(folderUri: Uri, displayName: String) {
        _state.value = _state.value.copy(
            isLoading = true,
            currentFolder = folderUri,
            title = displayName,
            permissionLost = false,
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { library.browse(folderUri) }
            }
            result.onSuccess { entries ->
                _state.value = _state.value.copy(isLoading = false, entries = entries)
            }
            result.onFailure { t ->
                if (t is SecurityException) {
                    app.settings.setRootUri(null)
                    _state.value = _state.value.copy(isLoading = false, permissionLost = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, entries = emptyList())
                }
            }
        }
    }
}

data class BrowserUiState(
    val isLoading: Boolean = true,
    val currentFolder: Uri? = null,
    val title: String = "",
    val entries: List<BrowseEntry> = emptyList(),
    val permissionLost: Boolean = false,
)
