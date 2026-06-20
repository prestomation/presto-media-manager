package com.presto.mediamanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.presto.mediamanager.data.model.MediaStatus

/**
 * A single captured video tracked through review. The SAF document URI is the
 * stable identity; everything else is metadata cached so the review feed and
 * the auto-delete job don't have to re-stat the folder constantly.
 */
@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    /** When the glasses recorded it (the document's last-modified time). */
    val dateCapturedMs: Long,
    /** When this app first discovered the file. Auto-delete counts from here. */
    val dateFirstSeenMs: Long,
    val status: MediaStatus,
    val reviewedAtMs: Long? = null,
)
