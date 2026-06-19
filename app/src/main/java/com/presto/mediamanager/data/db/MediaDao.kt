package com.presto.mediamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.presto.mediamanager.data.model.MediaStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    /** Items still needing attention, newest capture first. Drives the review feed. */
    @Query(
        "SELECT * FROM media_items WHERE status IN ('PENDING','LATER') " +
            "ORDER BY dateCapturedMs DESC",
    )
    fun observeReviewQueue(): Flow<List<MediaItem>>

    @Query("SELECT COUNT(*) FROM media_items WHERE status IN ('PENDING','LATER')")
    fun observeReviewCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE status IN ('PENDING','LATER')")
    suspend fun reviewCount(): Int

    @Query("SELECT * FROM media_items WHERE uri = :uri")
    suspend fun findByUri(uri: String): MediaItem?

    @Query("SELECT uri FROM media_items")
    suspend fun allUris(): List<String>

    /** Unreviewed items first seen on or before [cutoffMs] — candidates for auto-delete. */
    @Query(
        "SELECT * FROM media_items WHERE status IN ('PENDING','LATER') " +
            "AND dateFirstSeenMs <= :cutoffMs",
    )
    suspend fun staleItems(cutoffMs: Long): List<MediaItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(item: MediaItem)

    @Upsert
    suspend fun upsert(item: MediaItem)

    @Query("UPDATE media_items SET status = :status, reviewedAtMs = :reviewedAtMs WHERE uri = :uri")
    suspend fun updateStatus(uri: String, status: MediaStatus, reviewedAtMs: Long?)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteRow(uri: String)
}
