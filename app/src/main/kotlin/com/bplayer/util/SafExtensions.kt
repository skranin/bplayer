package com.bplayer.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

fun ContentResolver.takePersistablePermission(uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    runCatching { takePersistableUriPermission(uri, flags) }
}

fun ContentResolver.releasePersistablePermission(uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    runCatching { releasePersistableUriPermission(uri, flags) }
}
