package com.presto.mediamanager.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.presto.mediamanager.util.MediaProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Production [StorageGateway] backed by the Storage Access Framework. Folder URIs
 * are SAF tree URIs the user granted; file URIs are document URIs.
 */
class SafStorageGateway(private val context: Context) : StorageGateway {

    override fun persistFolderPermission(folderUri: String) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(Uri.parse(folderUri), flags)
    }

    override suspend fun listVideos(folderUri: String): List<StoredVideo> =
        withContext(Dispatchers.IO) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
                ?: return@withContext emptyList()
            tree.listFiles()
                .filter { it.isFile && it.type?.startsWith("video/") == true }
                .map {
                    StoredVideo(
                        uri = it.uri.toString(),
                        displayName = it.name ?: "video",
                        sizeBytes = it.length(),
                        lastModifiedMs = it.lastModified(),
                    )
                }
        }

    override suspend fun delete(uri: String): Boolean = withContext(Dispatchers.IO) {
        DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(uri))
    }

    override suspend fun copyInto(
        sourceUri: String,
        destFolderUri: String,
        fileName: String,
    ): String? = withContext(Dispatchers.IO) {
        val dest = createDocument(destFolderUri, fileName) ?: return@withContext null
        context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
            context.contentResolver.openOutputStream(dest)?.use { output ->
                input.copyTo(output)
            } ?: return@withContext null
        } ?: return@withContext null
        dest.toString()
    }

    override suspend fun writeFileInto(
        file: File,
        destFolderUri: String,
        fileName: String,
    ): String? = withContext(Dispatchers.IO) {
        val dest = createDocument(destFolderUri, fileName) ?: return@withContext null
        file.inputStream().use { input ->
            context.contentResolver.openOutputStream(dest)?.use { output ->
                input.copyTo(output)
            } ?: return@withContext null
        }
        dest.toString()
    }

    override suspend fun probeDurationMs(uri: String): Long =
        MediaProbe.durationMs(context, Uri.parse(uri))

    private fun createDocument(
        folderUri: String,
        fileName: String,
        mimeType: String = "video/mp4",
    ): Uri? {
        val treeUri = Uri.parse(folderUri)
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return DocumentsContract.createDocument(context.contentResolver, parent, mimeType, fileName)
    }
}
