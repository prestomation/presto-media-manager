package com.presto.mediamanager.data.settings

/** Target heights offered for the downscaled "share" copy. */
enum class ShareResolution(val label: String, val height: Int) {
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    ;

    companion object {
        fun fromHeight(height: Int): ShareResolution =
            entries.firstOrNull { it.height == height } ?: P720
    }
}

/** Playback speeds offered on the review feed. */
enum class PlaybackSpeed(val label: String, val multiplier: Float) {
    X0_5("0.5x", 0.5f),
    X1_0("1x", 1.0f),
    X1_5("1.5x", 1.5f),
    X2_0("2x", 2.0f),
    ;

    companion object {
        fun fromMultiplier(multiplier: Float): PlaybackSpeed =
            entries.firstOrNull { it.multiplier == multiplier } ?: X1_5
    }
}

/**
 * All persisted user configuration. Folder references are SAF tree URIs (as
 * strings) for which the app holds persistable read/write permission.
 */
data class AppSettings(
    val inputFolderUri: String? = null,
    val archiveFolderUri: String? = null,
    val shareFolderUri: String? = null,
    val autoDeleteEnabled: Boolean = false,
    val autoDeleteDays: Int = 14,
    val defaultRemoveAudio: Boolean = false,
    val defaultShareResolution: ShareResolution = ShareResolution.P720,
    val defaultPlaybackSpeed: PlaybackSpeed = PlaybackSpeed.X1_5,
) {
    val isConfigured: Boolean
        get() = inputFolderUri != null && archiveFolderUri != null && shareFolderUri != null
}
