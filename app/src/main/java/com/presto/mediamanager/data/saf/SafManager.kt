package com.presto.mediamanager.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.presto.mediamanager.util.MediaProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A video discovered inside a watched SAF tree. */
data class SafVideo(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
)

/**
 * Thin wrapper around the Storage Access Framework. All folder access goes
 * through tree URIs the user granted via [Intent.ACTION_OPEN_DOCUMENT_TREE],
 * with persistable permission so it survives reboots.
 */
class SafManager(private val context: Context) {

    /** Take long-lived read/write permission on a freshly picked tree. */
    fun persistTreePermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, flags)
    }

    /** List every video directly inside [treeUri]. */
    suspend fun listVideos(treeUri: Uri): List<SafVideo> = withContext(Dispatchers.IO) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        tree.listFiles()
            .filter { it.isFile && it.type?.startsWith("video/") == true }
            .map {
                SafVideo(
                    uri = it.uri.toString(),
                    displayName = it.name ?: "video",
                    sizeBytes = it.length(),
                    lastModifiedMs = it.lastModified(),
                )
            }
    }

    suspend fun delete(documentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        DocumentsContract.deleteDocument(context.contentResolver, documentUri)
    }

    /**
     * Copy [sourceUri] into [destTreeUri] under [fileName]. Used by the quick
     * Archive action (no re-encode), which copies the original bytes verbatim.
     */
    suspend fun copyInto(
        sourceUri: Uri,
        destTreeUri: Uri,
        fileName: String,
        mimeType: String = "video/mp4",
    ): Uri? = withContext(Dispatchers.IO) {
        val dest = createDocument(destTreeUri, fileName, mimeType) ?: return@withContext null
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(dest)?.use { output ->
                input.copyTo(output)
            } ?: return@withContext null
        } ?: return@withContext null
        dest
    }

    /** Write a local [file] (e.g. a Transformer output) into [destTreeUri]. */
    suspend fun writeFileInto(
        file: File,
        destTreeUri: Uri,
        fileName: String,
        mimeType: String = "video/mp4",
    ): Uri? = withContext(Dispatchers.IO) {
        val dest = createDocument(destTreeUri, fileName, mimeType) ?: return@withContext null
        file.inputStream().use { input ->
            context.contentResolver.openOutputStream(dest)?.use { output ->
                input.copyTo(output)
            } ?: return@withContext null
        }
        dest
    }

    private fun createDocument(treeUri: Uri, fileName: String, mimeType: String): Uri? {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return DocumentsContract.createDocument(context.contentResolver, parent, mimeType, fileName)
    }

    /** Best-effort duration probe for a freshly discovered document. */
    suspend fun probeDurationMs(uri: Uri): Long = MediaProbe.durationMs(context, uri)
}
