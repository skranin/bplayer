package com.bplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class Settings(private val context: Context) {
    private val rootUriKey = stringPreferencesKey("root_uri")

    val rootUri: Flow<String?> = context.dataStore.data.map { it[rootUriKey] }

    suspend fun setRootUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(rootUriKey) else prefs[rootUriKey] = uri
        }
    }
}
