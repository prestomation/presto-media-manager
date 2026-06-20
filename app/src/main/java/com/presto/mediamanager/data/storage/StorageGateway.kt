package com.presto.mediamanager.data.storage

import java.io.File

/** A video discovered inside a watched folder. URIs are opaque strings. */
data class StoredVideo(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
)

/**
 * Storage port the app talks to for all folder/file access. The production
 * implementation is SAF-backed ([SafStorageGateway]); tests use a temp-directory
 * fake so the repository's scan/archive/delete logic runs on the plain JVM.
 *
 * Folder and file references are opaque [String] URIs so callers (notably
 * [com.presto.mediamanager.data.repo.MediaRepository]) need no Android types.
 */
interface StorageGateway {
    /** Take long-lived permission on a freshly picked folder (no-op for fakes). */
    fun persistFolderPermission(folderUri: String)

    suspend fun listVideos(folderUri: String): List<StoredVideo>

    suspend fun delete(uri: String): Boolean

    /** Copy [sourceUri] into [destFolderUri] under [fileName]; returns the new URI. */
    suspend fun copyInto(sourceUri: String, destFolderUri: String, fileName: String): String?

    /** Write a local [file] (e.g. a Transformer output) into [destFolderUri]. */
    suspend fun writeFileInto(file: File, destFolderUri: String, fileName: String): String?

    suspend fun probeDurationMs(uri: String): Long
}
