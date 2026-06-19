package com.presto.mediamanager.data.model

/** Lifecycle state of a captured video as it moves through review. */
enum class MediaStatus {
    /** Newly discovered in the input folder, awaiting review. */
    PENDING,

    /** Explicitly deferred by the user; stays in the input folder, still counts toward auto-delete. */
    LATER,

    /** Copied (full resolution) into the archive folder and removed from input. */
    ARCHIVED,

    /** Shared (downscaled) and archived (full-res). Removed from input. */
    SHARED,

    /** Deleted from the input folder. */
    DELETED,
}
