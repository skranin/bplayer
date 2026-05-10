package com.bplayer.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri

private const val TAG = "BPlayer"

/**
 * Reads folder children directly via DocumentsContract — far faster than DocumentFile.listFiles()
 * which does a query per child. Single bulk query per folder.
 */
class Library(private val context: Context) {

    private val resolver get() = context.contentResolver

    /**
     * Delete a document (file or directory). Folders are deleted recursively by the SAF
     * provider. Returns true on success.
     */
    fun delete(docUri: Uri): Boolean = try {
        DocumentsContract.deleteDocument(resolver, docUri)
    } catch (e: Exception) {
        Log.w(TAG, "delete failed for $docUri", e)
        false
    }

    /** Display name of a single document URI; returns "" if not found. */
    fun displayName(docUri: Uri): String {
        val cols = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        resolver.query(docUri, cols, null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0) ?: ""
        }
        return ""
    }

    /** List the children of [folderUri] (a tree document URI). */
    fun listChildren(folderUri: Uri): List<RawDoc> {
        val parentDocId = try {
            if (DocumentsContract.isTreeUri(folderUri) &&
                !DocumentsContract.isDocumentUri(context, folderUri)
            ) {
                DocumentsContract.getTreeDocumentId(folderUri)
            } else {
                DocumentsContract.getDocumentId(folderUri)
            }
        } catch (e: Exception) {
            Log.w(TAG, "listChildren: getDocumentId failed for $folderUri", e)
            return emptyList()
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, parentDocId)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val out = ArrayList<RawDoc>()
        try {
            resolver.query(childrenUri, cols, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                while (c.moveToNext()) {
                    val id = c.getString(idIdx)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, id)
                    out += RawDoc(
                        uri = docUri,
                        name = c.getString(nameIdx) ?: "",
                        mimeType = c.getString(mimeIdx) ?: "",
                        size = if (c.isNull(sizeIdx)) 0L else c.getLong(sizeIdx),
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "listChildren: query failed for $folderUri", e)
        }
        return out
    }

    /** Walk one folder level and classify children for the browser screen. */
    fun browse(folderUri: Uri): List<BrowseEntry> {
        val children = listChildren(folderUri)
        Log.d(TAG, "browse: $folderUri -> ${children.size} children")
        val mimeCounts = children.groupingBy { it.mimeType }.eachCount()
        Log.d(TAG, "browse: mime counts = $mimeCounts")

        val folders = ArrayList<RawDoc>()
        val audio = ArrayList<RawDoc>()
        val images = ArrayList<RawDoc>()
        val unknown = ArrayList<RawDoc>()

        for (d in children) {
            when {
                d.name.startsWith(".") -> { /* skip dotfiles/dotfolders: .DS_Store, .stfolder, .thumbnails, .nomedia */ }
                d.isDir -> folders += d
                d.isAudio -> audio += d
                d.isImage -> images += d
                else -> unknown += d
            }
        }
        Log.d(TAG, "browse: folders=${folders.size} audio=${audio.size} images=${images.size} unknown=${unknown.size}")
        if (unknown.isNotEmpty()) {
            Log.d(TAG, "browse: unknown sample = ${unknown.take(5).map { "${it.name}(${it.mimeType})" }}")
        }

        val out = ArrayList<BrowseEntry>()

        audio.sortedWith(compareBy(NaturalRuComparator) { it.name }).forEach {
            out += BrowseEntry.SingleFileBook(it.uri, it.name, it.mimeType)
        }

        folders.sortedWith(compareBy(NaturalRuComparator) { it.name }).forEach { f ->
            val sub = try {
                listChildren(f.uri)
            } catch (e: Exception) {
                Log.w(TAG, "browse: peek failed for ${f.name}", e)
                emptyList()
            }
            val hasSubfolders = sub.any { it.isDir }
            val hasAudio = sub.any { it.isAudio }
            val coverUri = pickCover(sub)
            // Drill-in wins: a folder with subfolders is always navigable, even if it also has
            // loose audio at its top level (those become SingleFileBook tiles inside).
            out += when {
                hasSubfolders -> BrowseEntry.Group(
                    uri = f.uri, name = f.name, coverUri = coverUri,
                )
                hasAudio -> BrowseEntry.Book(
                    uri = f.uri,
                    name = f.name,
                    coverUri = coverUri,
                    trackCount = sub.count { it.isAudio },
                )
                else -> BrowseEntry.Group(
                    uri = f.uri, name = f.name, coverUri = coverUri,
                )
            }
        }

        Log.d(TAG, "browse: emitted ${out.size} entries")
        return out
    }

    /** Audio files in [folderUri], naturally sorted — used to build a play queue. */
    fun audioTracks(folderUri: Uri): List<TrackFile> =
        listChildren(folderUri)
            .filter { it.isAudio }
            .sortedWith(compareBy(NaturalRuComparator) { it.name })
            .map { TrackFile(it.uri, it.name, it.mimeType) }

    /** Pick a cover image from a list of children. */
    fun pickCover(children: List<RawDoc>): Uri? {
        val images = children.filter { it.isImage }
        if (images.isEmpty()) return null
        val priority = listOf("cover", "front", "folder", "albumart", "album")
        for (key in priority) {
            images.firstOrNull { it.name.substringBeforeLast('.').equals(key, ignoreCase = true) }
                ?.let { return it.uri }
        }
        // Fallback: first image alphabetically.
        return images.minWithOrNull(compareBy(NaturalRuComparator) { it.name })?.uri
    }
}

data class RawDoc(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
) {
    val isDir: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/") || extensionMatches(audioExtensions)
    val isImage: Boolean
        get() = mimeType.startsWith("image/") || extensionMatches(imageExtensions)

    private fun extensionMatches(set: Set<String>): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in set
    }
}

private val audioExtensions = setOf("mp3", "m4a", "m4b", "flac", "ogg", "opus", "wav", "aac")
private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")

fun String.toUriOrNull(): Uri? = runCatching { this.toUri() }.getOrNull()
