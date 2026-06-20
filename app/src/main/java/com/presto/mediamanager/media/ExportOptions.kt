package com.presto.mediamanager.media

import com.presto.mediamanager.data.settings.ShareResolution

/** Normalized crop rectangle in [0,1] of the source frame. (0,0)=top-left. */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val isFullFrame: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f
}

/** A user-selected trim window in source time. */
data class TrimRange(
    val startMs: Long,
    val endMs: Long,
)

/** Where the editor should send its output(s). */
enum class ExportDestination {
    /** Full-res edited copy → archive folder only. */
    ARCHIVE,

    /** Full-res → archive AND a downscaled copy → share folder. */
    SHARE,
}

/**
 * Everything needed to render an edited clip. The full-resolution edited file
 * always lands in the archive; the downscaled file only when destination is SHARE.
 */
data class ExportRequest(
    val label: String,
    val trim: TrimRange,
    val crop: CropRect,
    val removeAudio: Boolean,
    val shareResolution: ShareResolution,
    val destination: ExportDestination,
)
